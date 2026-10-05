import { test, expect } from "@playwright/test";
import { mockBrowserSession } from "./auth-fixture";
test("discards legacy persisted credentials instead of authorizing private requests", async ({page}) => {
  await page.addInitScript(()=>localStorage.setItem("uitmerch-auth",JSON.stringify({state:{accessToken:"LEGACY_ACCESS_SECRET",refreshToken:"LEGACY_REFRESH_SECRET",user:{id:"legacy",role:"ADMIN"}},version:0})));
  await mockBrowserSession(page);
  const privateRequests:string[]=[];
  await page.route("**/api/v1/customer/**",async route=>{privateRequests.push(route.request().url());await route.fulfill({status:401,json:{success:false}});});
  await page.route("**/api/v1/public/**",route=>route.fulfill({json:{success:true,data:[]}}));
  await page.goto("/following");
  await expect(page.getByRole("heading",{name:"Truy cập tài khoản của bạn"})).toBeVisible();
  expect(await page.evaluate(()=>localStorage.getItem("uitmerch-auth"))).toBeNull();
  expect(privateRequests).toHaveLength(0);
});
test("reload restores from an HttpOnly cookie without persisting access or refresh tokens", async ({page,context})=>{
  await mockBrowserSession(page,{id:"secure-user",email:"secure@example.test",fullName:"Secure User",role:"CUSTOMER",isVerified:true});
  await page.route("**/api/v1/**",async route=>{
    const path=new URL(route.request().url()).pathname;
    if(path.startsWith("/api/v1/auth/")) return route.fallback();
    if(path.endsWith("/stream")) {
      expect(new URL(route.request().url()).searchParams.has("token")).toBe(false);
      expect(route.request().headers().authorization).toMatch(/^Bearer /);
      return route.fulfill({contentType:"text/event-stream",body:""});
    }
    return route.fulfill({json:{success:true,data:path.endsWith("unread-count")?{unreadCount:0}:[]}});
  });
  await page.goto("/following");
  await expect(page.getByRole("heading",{name:"Tổ chức bạn theo dõi"})).toBeVisible();
  await expect(page.getByRole("link",{name:"Đăng nhập",exact:true})).toHaveCount(0);
  await page.reload();
  await expect(page.getByRole("heading",{name:"Tổ chức bạn theo dõi"})).toBeVisible();
  await expect(page.getByRole("link",{name:"Đăng nhập",exact:true})).toHaveCount(0);
  expect(await page.evaluate(()=>document.cookie)).not.toContain("uitmerch-refresh");
  expect(await page.evaluate(()=>localStorage.getItem("uitmerch-auth"))).toBeNull();
  expect((await context.cookies()).find(cookie=>cookie.name==="uitmerch-refresh")?.httpOnly).toBe(true);
});
