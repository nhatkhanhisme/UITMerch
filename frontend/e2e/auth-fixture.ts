import type { Page } from "@playwright/test";
export type TestUser = { id: string; email: string; fullName: string; role: string; isVerified: boolean; avatarUrl?: string };
export const accessToken = (seconds=3600) => `header.${Buffer.from(JSON.stringify({ exp:Math.floor(Date.now()/1000)+seconds })).toString("base64url")}.signature`;
/** The browser bootstraps through the cookie endpoints; credentials are never injected into storage. */
export async function mockBrowserSession(page: Page, user?: TestUser, token=accessToken()) {
  if(user) await page.context().addCookies([{name:"uitmerch-refresh",value:"test-http-only-refresh",domain:"127.0.0.1",path:"/api/v1/auth",httpOnly:true,sameSite:"Lax"}]);
  await page.route("**/api/v1/auth/**", async route => {
    const path=new URL(route.request().url()).pathname;
    if(path.endsWith("/csrf")) return route.fulfill({json:{success:true,data:{csrfToken:"test-csrf"}}});
    if(path.endsWith("/refresh") && user) return route.fulfill({json:{success:true,data:{token,tokenType:"Bearer",userId:user.id,...user}}});
    if(path.endsWith("/logout")) return route.fulfill({json:{success:true,data:null},headers:{"Set-Cookie":"uitmerch-refresh=; Path=/api/v1/auth; HttpOnly; SameSite=Lax; Max-Age=0"}});
    return route.fulfill({status:401,json:{success:false,message:"Invalid session"}});
  });
}
