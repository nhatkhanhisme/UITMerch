import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { HomeMoreLink } from "./HomeMoreLink";
import { getPublicOrganizations } from "../../api/organization";
import type { OrganizationResponse } from "../../types/shared";

function initials(name: string) {
  return name
    .split(" ")
    .map((word) => word[0])
    .join("")
    .slice(0, 3)
    .toUpperCase();
}

type OrgItem = {
  id: string;
  name: string;
  logoUrl?: string;
};

export function HomeOrgan() {
  const [orgs, setOrgs] = useState<OrgItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;
    setIsLoading(true);
    setError(null);

    async function loadOrgs() {
      try {
        const res = await getPublicOrganizations({ size: 7 });
        if (isActive) {
          if (res?.data && res.data.length > 0) {
            setOrgs(
              res.data.map((o: OrganizationResponse) => ({
                id: o.id,
                logoUrl: o.logoUrl,
                name: o.name,
              })),
            );
          } else {
            setError("Hiện tại chưa có tổ chức hay câu lạc bộ nào được đăng tải.");
          }
        }
      } catch {
        if (isActive) {
          setError("Không thể kết nối đến máy chủ để tải danh sách tổ chức.");
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    void loadOrgs();

    return () => {
      isActive = false;
    };
  }, []);

  return (
    <section
      className="
        relative isolate flex min-h-[100svh] items-center
        px-4 py-20
        sm:px-8 sm:py-24
        lg:px-16 lg:py-28
      "
      id="home-organ"
      data-section="home-organ"
    >
      <div className="relative z-10 mx-auto w-full max-w-canvas">
        <div className="mx-auto max-w-6xl text-center">
          <p className="font-sans text-sm font-semibold uppercase tracking-[0.22em] text-slate/70">
            Cộng đồng UIT
          </p>
          <h2 className="mb-5 mt-2 font-fredoka text-3xl font-bold leading-tight text-black-blue sm:text-4xl lg:whitespace-nowrap lg:text-[44px] xl:text-[56px] 2xl:text-[64px]">
            Khám phá merch từ các CLB và Khoa
          </h2>
        </div>

        {isLoading ? (
          <div className="mt-16 flex min-h-[30vh] flex-col items-center justify-center gap-5 text-center">
            <div className="relative flex size-16 items-center justify-center">
              <div className="absolute inset-0 rounded-full border-4 border-white/20" />
              <div className="absolute inset-0 animate-spin rounded-full border-4 border-transparent border-t-aqua" />
            </div>
            <p className="font-sans text-sm font-semibold tracking-wide text-slate/70 animate-pulse">
              Đang tải danh sách câu lạc bộ và khoa...
            </p>
          </div>
        ) : error || orgs.length === 0 ? (
          <div className="mt-16 flex min-h-[30vh] flex-col items-center justify-center text-center">
            <div className="mx-auto max-w-md rounded-[32px] border border-white/50 bg-white/30 p-8 shadow-glass backdrop-blur-xl">
              <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-aqua/10 text-aqua mb-4">
                ⚠
              </div>
              <p className="font-fredoka text-xl font-bold text-black-blue">
                {error || "Chưa có danh sách tổ chức."}
              </p>
              <p className="mt-3 font-sans text-sm leading-6 text-ink/65">
                Vui lòng kiểm tra lại đường truyền mạng hoặc truy cập trang danh sách tổ chức để xem các đơn vị trực thuộc UIT.
              </p>
            </div>
          </div>
        ) : (
          <>
            <div className="mx-auto mt-10 grid max-w-5xl grid-cols-2 gap-x-8 gap-y-10 sm:gap-x-10 sm:gap-y-12 md:grid-cols-3 lg:grid-cols-4">
              {orgs.map((org) => (
                <Link
                  aria-label={`Xem chi tiết ${org.name}`}
                  className="group flex min-w-0 flex-col items-center justify-center gap-3 bg-transparent transition duration-200 ease-out hover:-translate-y-1 focus-visible:outline-aqua"
                  key={org.id}
                  to={`/organization/${org.id}`}
                >
                  <div className="relative h-36 w-36 sm:h-40 sm:w-40">
                    <div className="relative z-10 inline-flex h-full w-full items-center justify-center overflow-hidden rounded-full border border-white/70 bg-white/20 shadow-[0_10px_30px_rgba(82,128,145,0.10),inset_1.5px_1.5px_5px_rgba(255,255,255,0.95),inset_-1px_-1px_3px_rgba(255,255,255,0.35)] backdrop-blur-[6px]">
                      {org.logoUrl ? (
                        <img
                          alt={org.name}
                          className="absolute inset-0 m-auto size-3/5 object-contain mix-blend-multiply opacity-90 transition duration-300 group-hover:scale-105 group-hover:opacity-100"
                          src={org.logoUrl}
                        />
                      ) : (
                        <span className="text-base font-bold text-slate-700">
                          {initials(org.name)}
                        </span>
                      )}
                    </div>
                  </div>
                  <h3 className="mt-1 w-full truncate text-center text-[13px] font-bold text-slate-700 sm:text-sm group-hover:text-black-blue">
                    {org.name}
                  </h3>
                </Link>
              ))}

              <Link
                aria-label="Xem tất cả đơn vị"
                className="group flex min-w-0 flex-col items-center justify-center gap-3 bg-transparent transition duration-200 ease-out hover:-translate-y-1 focus-visible:outline-aqua"
                to="/organization"
              >
                <div className="relative h-36 w-36 sm:h-40 sm:w-40">
                  <div className="relative z-10 inline-flex h-full w-full items-center justify-center rounded-full border border-white/70 bg-white/20 shadow-[0_10px_30px_rgba(82,128,145,0.10),inset_1.5px_1.5px_5px_rgba(255,255,255,0.95),inset_-1px_-1px_3px_rgba(255,255,255,0.35)] backdrop-blur-[6px]">
                    <span className="text-base font-bold text-slate-700">...</span>
                  </div>
                </div>
                <p className="mt-1 w-full truncate text-center text-[13px] font-bold text-slate-700 sm:text-sm group-hover:text-black-blue">
                  Nhiều đơn vị khác
                </p>
              </Link>
            </div>

            <div className="mt-16 flex justify-center">
              <HomeMoreLink ariaLabel="Xem thêm cộng đồng UIT" to="/organization" />
            </div>
          </>
        )}
      </div>
    </section>
  );
}
