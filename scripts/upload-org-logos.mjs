/**
 * Upload logo của từng CLB / Khoa lên Supabase Storage.
 * Path: org-assets/organizations/{org-uuid}/logo/{slug}
 *
 * Chạy một lần từ máy có kết nối internet:
 *   cd scripts && npm init -y && npm install @aws-sdk/client-s3 && node upload-org-logos.mjs
 *
 * Credentials đọc từ backend/.env tự động.
 */

import { S3Client, PutObjectCommand, HeadBucketCommand } from "@aws-sdk/client-s3";
import { readFileSync, existsSync } from "fs";
import { extname, resolve, dirname } from "path";
import { fileURLToPath } from "url";

const __dir   = dirname(fileURLToPath(import.meta.url));
const envPath = resolve(__dir, "../backend/.env");
const env     = Object.fromEntries(
  readFileSync(envPath, "utf8")
    .split("\n")
    .filter(l => l.includes("=") && !l.startsWith("#"))
    .map(l => { const i = l.indexOf("="); return [l.slice(0, i).trim(), l.slice(i + 1).trim()]; })
);

const ENDPOINT = env["SUPABASE_STORAGE_ENDPOINT"];
const REGION   = env["SUPABASE_STORAGE_REGION"];
const ACCESS   = env["SUPABASE_STORAGE_S3_ACCESS_KEY_ID"];
const SECRET   = env["SUPABASE_STORAGE_S3_SECRET_KEY"];
const PROJECT  = env["SUPABASE_PROJECT_URL"];
const BUCKET   = "org-assets";

const BASE = resolve(__dir, "../backend/src/main/resources/data/[IMAGE] UIT DATA INFO");

// org-uuid khớp với production DB (SELECT id, name FROM organizations)
const IMAGES = [
  // Trường / Khoa
  { orgId: "6a637d6f-5fe9-4d12-bad6-199761e5b899", slug: "uit.png",            file: `${BASE}/Trường-Khoa/uit logo.png`          },
  { orgId: "2dca50c1-4717-4523-bf40-26e41f5f2d28", slug: "khmt-uit.png",       file: `${BASE}/Trường-Khoa/khmt-uit logo.png`     },
  { orgId: "1dbae61f-ed0b-4b28-a523-6e92c241e602", slug: "ktmt-fce.jpg",       file: `${BASE}/Trường-Khoa/ktmt-fce-uit logo.jpg` },
  { orgId: "cebfa619-8787-46c9-9151-5bbc88787853", slug: "se-uit.png",         file: `${BASE}/Trường-Khoa/se-uit logo.png`       },
  { orgId: "ad88bd53-a52a-4a56-a23e-d53e3183c2a2", slug: "khkttt-ise.png",     file: `${BASE}/Trường-Khoa/khkttt-ise logo.png`   },
  { orgId: "c36ed407-657c-423b-880e-d4c862a3d844", slug: "httt.png",           file: `${BASE}/Trường-Khoa/httt logo.png`         },
  { orgId: "a1cd7527-f704-47cd-b2fd-43a2b3da0321", slug: "mmt-tt.png",         file: `${BASE}/Trường-Khoa/mmt-tt logo.png`       },
  // UIT Store
  { orgId: "4eb1edcf-9e4a-4912-85c4-80b133d02451", slug: "uit.png",            file: `${BASE}/Trường-Khoa/uit logo.png`          },
  // CLB học thuật
  { orgId: "9bc44f87-e948-440b-8510-d8e4c6220f06", slug: "cs-uit-ai.png",      file: `${BASE}/CLB/cs-uit ai logo.png`            },
  { orgId: "d8b44d0a-d7d8-4858-8a0c-fc5e0b656968", slug: "csac.jpg",           file: `${BASE}/CLB/csac logo.jpg`                 },
  { orgId: "b8110173-bad6-433e-98d5-9ff0c78d95e3", slug: "ceec.jpg",           file: `${BASE}/CLB/ceec logo.jpg`                 },
  { orgId: "8de613f0-d15b-4f93-860a-64b7538839ae", slug: "ic-uit.jpg",         file: `${BASE}/CLB/ic uit logo.jpg`               },
  { orgId: "69aa1606-d876-4c57-8259-dd30fcad4ede", slug: "gamapp.jpg",         file: `${BASE}/CLB/gamapp studios logo.jpg`       },
  { orgId: "539f5e72-39cc-4425-b001-28fc60fa00de", slug: "gdgoc.jpg",          file: `${BASE}/CLB/gdgoc-uit logo.jpg`            },
  { orgId: "23300c90-3205-4e2f-90ce-f610b49cddbf", slug: "webdev.jpg",         file: `${BASE}/CLB/webdev logo.jpg`               },
  // CLB truyền thông / văn nghệ
  { orgId: "c8b881bf-1c39-4bfe-b8a2-8f6ada4fee35", slug: "artistry.jpg",       file: `${BASE}/CLB/artistry logo.jpg`             },
  { orgId: "bdd8200e-3671-4297-ba20-3b3bf4d00007", slug: "uma.jpg",            file: `${BASE}/CLB/uma logo.jpg`                  },
  { orgId: "a6ecfd83-8436-4a7a-b26f-1296ef337892", slug: "lossless.jpg",       file: `${BASE}/CLB/lossless uit logo.jpg`         },
  { orgId: "678f73f3-eb46-4454-acd1-b69bffe609e5", slug: "event-uit.jpg",      file: `${BASE}/CLB/event uit logo.jpg`            },
  // CLB ngoại ngữ / xã hội
  { orgId: "3e7e579f-f318-45db-8b60-d4787ae252fd", slug: "oec.jpg",            file: `${BASE}/CLB/oec logo.jpg`                  },
  { orgId: "6e4b0555-e8c0-4a0d-9ffd-0c0a6c9753b5", slug: "wakame.jpg",         file: `${BASE}/CLB/wakame logo.jpg`               },
  { orgId: "f153f5da-81a7-4799-9768-9fa6d37def6f", slug: "ly-luan-tre.jpg",    file: `${BASE}/CLB/ly-luan-tre logo.jpg`          },
  { orgId: "73539ddb-ce62-4785-b53d-007c36e64475", slug: "sv5t.jpg",           file: `${BASE}/CLB/sv5t uit logo.jpg`             },
  { orgId: "2dbce5ec-7aa2-4fc1-842a-cc2355844683", slug: "ctxh.jpg",           file: `${BASE}/CLB/ctxh uit logo.jpg`             },
  { orgId: "d227dc34-9a46-4ca9-b136-51ecede1f5f5", slug: "uit-leader.jpg",     file: `${BASE}/CLB/uit-leader logo 1.jpg`         },
  // Tình nguyện
  { orgId: "f6ddd4e5-42e6-4329-b7a8-01e051484198", slug: "clb-sach-hanh-dong.jpg", file: `${BASE}/CLB/clb-sach-hanh-dong logo.jpg` },
  { orgId: "e4445bab-248d-4828-9daa-1cc0def8f796", slug: "mtc-ttm.jpg",        file: `${BASE}/CLB/mtc-ttm logo.jpg`              },
  // CLB thể thao
  { orgId: "3cd1e2ea-d828-49c0-ab20-3daa7cf374fa", slug: "basketball.jpg",     file: `${BASE}/CLB/basketball logo.jpg`           },
  { orgId: "0ba3db46-608c-4d81-b527-684b3dc07ddb", slug: "badminton.jpg",      file: `${BASE}/CLB/badminton logo.jpg`            },
  { orgId: "f6ed9f2a-8f98-418b-80fe-c78814d4d859", slug: "futsal.jpg",         file: `${BASE}/CLB/futsal logo.jpg`               },
  { orgId: "6c55a805-de7f-46bf-b937-8d269a867669", slug: "uvc.jpg",            file: `${BASE}/CLB/uvc logo.jpg`                  },
  { orgId: "757b9763-f916-4963-b311-d691db0f8411", slug: "taekwondo.jpg",      file: `${BASE}/CLB/taekwondo logo.jpg`            },
  { orgId: "5a9643c0-55f3-4140-918c-80f050465133", slug: "uit-chess.jpg",      file: `${BASE}/CLB/uit chess logo.jpg`            },
];

const s3 = new S3Client({
  endpoint: ENDPOINT,
  region: REGION,
  credentials: { accessKeyId: ACCESS, secretAccessKey: SECRET },
  forcePathStyle: true,
});

function mimeFor(slug) {
  return extname(slug).toLowerCase() === ".png" ? "image/png" : "image/jpeg";
}

async function upload({ orgId, slug, file }) {
  if (!existsSync(file)) {
    console.warn(`  SKIP (not found): ${file}`);
    return;
  }
  const key = `organizations/${orgId}/logo/${slug}`;
  await s3.send(new PutObjectCommand({
    Bucket: BUCKET,
    Key: key,
    Body: readFileSync(file),
    ContentType: mimeFor(slug),
  }));
  console.log(`  OK  ${slug}  (${orgId})`);
}

(async () => {
  console.log(`Endpoint : ${ENDPOINT}`);
  console.log(`Project  : ${PROJECT}`);
  console.log(`Uploading ${IMAGES.length} logos to ${BUCKET}/organizations/{id}/logo/...\n`);
  for (const img of IMAGES) {
    await upload(img);
  }
  console.log("\nDone.");
})();
