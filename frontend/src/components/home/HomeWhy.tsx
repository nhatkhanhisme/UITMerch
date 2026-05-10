const reasons = [
  {
    title: "Thiết kế đúng tinh thần UIT",
    body: "Mỗi sản phẩm được chọn lọc để giữ chất công nghệ, trẻ trung và dễ dùng trong đời sống sinh viên."
  },
  {
    title: "Đặt hàng minh bạch",
    body: "Thông tin sản phẩm, đơn vị phát hành và trạng thái mở bán được trình bày rõ để bạn chọn nhanh hơn."
  },
  {
    title: "Ủng hộ cộng đồng",
    body: "Mua merch cũng là cách đồng hành cùng hoạt động học thuật, sự kiện và câu lạc bộ trong trường."
  }
];

export function HomeWhy() {
  return (
    <section
      className="relative scroll-mt-16 overflow-hidden bg-canvas px-5 py-16 sm:scroll-mt-20 sm:px-8 sm:py-20 lg:px-16 lg:py-24"
      data-section="home-why"
    >
      <div className="absolute inset-x-0 top-1/2 h-px bg-white/80" />
      {/* RESPONSIVE */}
      <div className="relative mx-auto grid max-w-canvas gap-10 pt-16 sm:pt-20 lg:grid-cols-[0.75fr_1.25fr] lg:items-end lg:gap-12 lg:pt-24">
        <div>
          <p className="font-google text-sm font-semibold uppercase tracking-[0.22em] text-slate/70">
            Why UITMerch
          </p>
          {/* RESPONSIVE */}
          <h2 className="mt-4 font-brand text-[34px] font-extrabold leading-tight text-black-blue sm:text-5xl lg:text-[68px]">
            Một nơi gom đủ tinh thần UIT
          </h2>
          <p className="mt-6 font-google text-sm leading-7 text-gray sm:text-base sm:leading-8">
            UITMerch giúp sinh viên tìm sản phẩm chính thức từ các khoa, câu
            lạc bộ và sự kiện trong trường mà không phải lạc giữa nhiều kênh
            đăng bán rời rạc.
          </p>
        </div>

        {/* RESPONSIVE */}
        <div className="grid gap-4 sm:grid-cols-3 sm:gap-5">
          {reasons.map((reason, index) => (
            <article
              className="rounded-[24px] border border-white/75 bg-white/35 p-5 shadow-[12px_18px_50px_rgba(82,128,145,0.13),inset_1.5px_1.5px_10px_rgba(255,255,255,0.75)] backdrop-blur transition duration-200 ease-out hover:-translate-y-1 hover:bg-white/50 sm:rounded-[32px] sm:p-6"
              key={reason.title}
            >
              <span className="font-display text-3xl text-peach sm:text-4xl">
                0{index + 1}
              </span>
              <h3 className="mt-6 font-google text-lg font-bold leading-7 text-black-blue sm:mt-8 sm:text-xl">
                {reason.title}
              </h3>
              <p className="mt-4 font-google text-sm leading-7 text-gray">
                {reason.body}
              </p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
