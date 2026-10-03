#!/usr/bin/env python3
"""Add isolated demo rows; credentials stay in a private, ignored local file."""
import argparse
import json
import os
from pathlib import Path
import secrets
import shlex
import subprocess
from urllib.parse import urlsplit, parse_qs

BACKEND = Path(__file__).resolve().parents[1]
ACCOUNTS = {role: f"demo.{role}@uitmerch.test" for role in ("customer", "organizer", "admin", "inactive")}


def database_environment(mode):
    env = os.environ.copy()
    if mode == "configured":
        values = {}
        for line in (BACKEND / ".env").read_text().splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.removeprefix("export ").split("=", 1)
            parts = shlex.split(value, comments=True)
            values[key.strip()] = " ".join(parts)
        values.update({k: v for k, v in env.items() if k.startswith("SPRING_DATASOURCE_")})
        url = urlsplit(values["SPRING_DATASOURCE_URL"].removeprefix("jdbc:"))
        env.update(PGHOST=url.hostname or "localhost", PGPORT=str(url.port or 5432),
                   PGDATABASE=url.path.lstrip("/"), PGUSER=values["SPRING_DATASOURCE_USERNAME"],
                   PGPASSWORD=values["SPRING_DATASOURCE_PASSWORD"],
                   PGSSLMODE=parse_qs(url.query).get("sslmode", ["prefer"])[0])
    env["PGCONNECT_TIMEOUT"] = "10"
    return env


def query(sql, env):
    result = subprocess.run(["psql", "-X", "-q", "-A", "-t", "-v", "ON_ERROR_STOP=1"],
                            input=sql, text=True, capture_output=True, env=env)
    if result.returncode:
        # Never include connection credentials or SQL containing the generated password.
        raise RuntimeError("Database operation failed; demo transaction was rolled back. Check connection and migrated schema.")
    return result.stdout.strip()


def literal(value):
    return "'" + value.replace("'", "''") + "'"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database", required=True, choices=("configured", "pg-env"),
                        help="configured reads backend/.env; pg-env uses PG* environment variables")
    parser.add_argument("--credentials", type=Path, default=BACKEND / ".demo-credentials.json")
    args = parser.parse_args()
    env = database_environment(args.database)
    existing = query("SELECT count(*) FROM users WHERE email LIKE 'demo.%@uitmerch.test';", env)
    credentials_path = args.credentials.resolve()
    if credentials_path.exists():
        credentials = json.loads(credentials_path.read_text())
        password = credentials["password"]
        if credentials.get("accounts") != ACCOUNTS:
            raise RuntimeError("Credentials file belongs to another demo dataset.")
    else:
        if existing != "0":
            raise RuntimeError("Demo accounts already exist. Supply their original private credentials file; passwords will not be reset.")
        credentials = {"accounts": ACCOUNTS, "password": secrets.token_urlsafe(24)}
        password = credentials["password"]
        credentials_path.parent.mkdir(parents=True, exist_ok=True)
        fd = os.open(credentials_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(fd, "w") as file:
            json.dump(credentials, file, ensure_ascii=False, indent=2)
            file.write("\n")
    os.chmod(credentials_path, 0o600)
    schema = query("SELECT n.nspname FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE p.proname='crypt' AND p.pronargs=2 LIMIT 1;", env)
    if not schema:
        raise RuntimeError("Database needs pgcrypto (crypt/gen_salt). No database extensions were changed.")
    identifier = '"' + schema.replace('"', '""') + '"'
    hashed = query(f"SELECT {identifier}.crypt({literal(password)}, {identifier}.gen_salt('bf',10));", env)
    sql = (BACKEND / "scripts" / "feature-demo.sql").read_text().replace("{{DEMO_PASSWORD_HASH}}", literal(hashed))
    query(sql, env)
    counts = query("SELECT json_build_object('users',(SELECT count(*) FROM users WHERE id::text LIKE 'f0100000-%'), 'organizations',(SELECT count(*) FROM organizations WHERE id::text LIKE 'f0200000-%'), 'products',(SELECT count(*) FROM merch_items WHERE id::text LIKE 'f0300000-%'), 'events',(SELECT count(*) FROM events WHERE id::text LIKE 'f0400000-%'), 'campaigns',(SELECT count(*) FROM preorder_campaigns WHERE id::text LIKE 'f0500000-%'), 'orders',(SELECT count(*) FROM orders WHERE id::text LIKE 'f0700000-%'));", env)
    print(counts)
    print(f"Private credentials: {credentials_path} (mode 0600; do not commit or share)")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, KeyError, ValueError) as error:
        raise SystemExit(str(error))
