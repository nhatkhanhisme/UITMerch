import { useEffect, useId, useRef, useState } from "react";
import { requestCheckoutCode, verifyCheckoutCode } from "../api/checkoutSecurity";
import { getApiErrorMessage } from "../api/auth";
export function GuestEmailVerification({ email, onVerified }: { email: string; onVerified: (token: string) => void }) {
  const inputId = useId();
  const verifiedHandler = useRef(onVerified); verifiedHandler.current = onVerified;
  const latestEmail = useRef(email);
  latestEmail.current = email;
  const [challenge, setChallenge] = useState("");
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [verified, setVerified] = useState(false);
  const [message, setMessage] = useState("");
  useEffect(() => { setChallenge(""); setCode(""); setVerified(false); setMessage(""); }, [email]);
  useEffect(() => {
    if (!verified) return;
    const expiry = setTimeout(() => { setVerified(false); verifiedHandler.current(""); setMessage("Phiên xác minh đã hết hạn. Vui lòng yêu cầu mã mới."); }, 30 * 60 * 1000);
    return () => clearTimeout(expiry);
  }, [verified, email]);
  async function send() {
    const requestedEmail = email;
    setBusy(true); setMessage(""); setVerified(false); if (verified) onVerified("");
    try { const id = await requestCheckoutCode(email.trim()); if (latestEmail.current !== requestedEmail) return; setChallenge(id); setMessage("Mã xác minh đã gửi đến email. Mã có hiệu lực 10 phút."); }
    catch (error) { if (latestEmail.current !== requestedEmail) return; setMessage(getApiErrorMessage(error, "Không thể gửi mã xác minh.")); }
    finally { setBusy(false); }
  }
  async function verify() {
    const requestedEmail = email;
    setBusy(true); setMessage("");
    try { const token = await verifyCheckoutCode(email.trim(), challenge, code.trim()); if (latestEmail.current !== requestedEmail) return; onVerified(token); setVerified(true); setCode(""); setMessage("Email đã xác minh. Bạn có thể đặt hàng trong 30 phút."); }
    catch (error) { if (latestEmail.current !== requestedEmail) return; setMessage(getApiErrorMessage(error, "Mã không đúng hoặc đã hết hạn.")); }
    finally { setBusy(false); }
  }
  return <section aria-label="Xác minh email đặt hàng" className="space-y-2">
    {<button type="button" disabled={busy || !email.trim()} onClick={send} className="rounded-xl border px-3 py-2 text-sm">{busy ? "Đang xử lý…" : verified ? "Xác minh lại email" : challenge ? "Gửi lại mã xác minh" : "Gửi mã xác minh email"}</button>}
    {!!challenge && !verified && <div className="block text-sm"><label htmlFor={inputId}>Mã xác minh 6 số</label>
      <input id={inputId} inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={e => setCode(e.target.value)} className="block w-full rounded-xl border p-2" />
      <button type="button" disabled={busy || !/^\d{6}$/.test(code)} onClick={verify} className="mt-2 rounded-xl border px-3 py-2">Xác minh</button>
    </div>}
    <p role="status" aria-live="polite" className="text-sm">{message}</p>
  </section>;
}
