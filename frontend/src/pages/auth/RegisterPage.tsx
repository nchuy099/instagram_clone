import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../lib/axios';

const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const DAYS = Array.from({ length: 31 }, (_, i) => i + 1);
const YEARS = Array.from({ length: 120 }, (_, i) => new Date().getFullYear() - i);

export default function RegisterPage() {
  const [mobileOrEmail, setMobileOrEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrors([]);

    try {
      await api.post('/auth/register', { mobileOrEmail, fullName, username, password });
      navigate('/login');
    } catch (err: any) {
      const data = err.response?.data;
      const errorDetails = data?.error?.details || data?.details;

      if (errorDetails && typeof errorDetails === 'object') {
        const detailMsgs = Object.values(errorDetails) as string[];
        setErrors(detailMsgs);
      } else {
        const errorMsg = data?.error?.message || data?.message || 'Registration failed. Please try again.';
        setErrors([errorMsg]);
      }
    } finally {
      setIsLoading(false);
    }
  };


  return (
    <div className="min-h-screen flex flex-col justify-center items-center bg-white md:bg-[#fafafa] py-12 px-4 selection:bg-blue-100 font-sans text-black">
      <div className="w-full max-w-[350px] flex flex-col gap-2.5">

        {/* Logo and Form Box */}
        <div className="bg-white px-10 pt-10 pb-5 md:border border-[#dbdbdb] rounded-[1px] flex flex-col items-center">
          <h1 className="text-[32px] font-bold text-black mb-6 tracking-tighter">Instagram</h1>

          <p className="text-[#8e8e8e] text-[17px] font-semibold text-center leading-[20px] mb-8">
            Sign up to see photos and videos from your friends.
          </p>

          <form onSubmit={handleSubmit} className="w-full flex flex-col gap-[6px]">
            <input
              id="email"
              type="text"
              required
              className="w-full px-2.5 py-[9px] border border-[#dbdbdb] rounded-[3px] bg-[#fafafa] text-[12px] text-black placeholder-[#8e8e8e] focus:outline-none focus:border-[#a8a8a8]"
              placeholder="Mobile number or email"
              value={mobileOrEmail}
              onChange={(e) => setMobileOrEmail(e.target.value)}
            />
            <input
              id="fullName"
              type="text"
              className="w-full px-2.5 py-[9px] border border-[#dbdbdb] rounded-[3px] bg-[#fafafa] text-[12px] text-black placeholder-[#8e8e8e] focus:outline-none focus:border-[#a8a8a8]"
              placeholder="Full Name"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />
            <input
              id="username"
              type="text"
              required
              className="w-full px-2.5 py-[9px] border border-[#dbdbdb] rounded-[3px] bg-[#fafafa] text-[12px] text-black placeholder-[#8e8e8e] focus:outline-none focus:border-[#a8a8a8]"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
            <div className="relative">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                required
                className="w-full px-2.5 py-[9px] pr-14 border border-[#dbdbdb] rounded-[3px] bg-[#fafafa] text-[12px] text-black placeholder-[#8e8e8e] focus:outline-none focus:border-[#a8a8a8]"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              {password.length > 0 && (
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[14px] font-semibold text-black hover:opacity-50"
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              )}
            </div>

            {/* Birthday Section */}
            <div className="mt-4 mb-2">
              <p className="text-[14px] font-semibold text-black mb-1">Birthday</p>
              <div className="flex gap-2">
                <select className="flex-1 px-2 py-2 border border-[#dbdbdb] rounded-sm bg-[#fafafa] text-[14px] text-black focus:outline-none appearance-none cursor-pointer">
                  {MONTHS.map((m) => (
                    <option key={m} value={m}>{m}</option>
                  ))}
                </select>
                <select className="flex-1 px-2 py-2 border border-[#dbdbdb] rounded-sm bg-[#fafafa] text-[14px] text-black focus:outline-none appearance-none cursor-pointer" defaultValue={new Date().getDate()}>
                  {DAYS.map((d) => (
                    <option key={d} value={d}>{d}</option>
                  ))}
                </select>
                <select className="flex-1 px-2 py-2 border border-[#dbdbdb] rounded-sm bg-[#fafafa] text-[14px] text-black focus:outline-none appearance-none cursor-pointer" defaultValue={new Date().getFullYear()}>
                  {YEARS.map((y) => (
                    <option key={y} value={y}>{y}</option>
                  ))}
                </select>
              </div>
              <p className="text-[12px] text-[#8e8e8e] text-center mt-2">This won't be part of your public profile.</p>
            </div>

            {/* Sign Up Button */}
            <button
              type="submit"
              disabled={isLoading}
              className={`w-full mt-2 py-[5px] text-white text-[14px] font-semibold rounded-[8px] transition-colors bg-[#0095f6] hover:bg-[#1877f2] ${isLoading ? 'opacity-70 cursor-default' : ''
                }`}
            >
              {isLoading ? 'Signing up...' : 'Sign Up'}
            </button>

            {errors.length > 0 && (
              <div className="mt-4 flex flex-col gap-1">
                {errors.map((err, index) => (
                  <p key={index} className="text-[#ed4956] text-[14px] text-center">{err}</p>
                ))}
              </div>
            )}



            {/* Terms */}
            <p className="text-[12px] text-[#8e8e8e] text-center mt-6 leading-4">
              By signing up, you agree to our <span className="font-semibold text-[#8e8e8e]">Terms</span>, <span className="font-semibold text-[#8e8e8e]">Privacy Policy</span> and <span className="font-semibold text-[#8e8e8e]">Cookies Policy</span>.
            </p>
          </form>
        </div>

        {/* Have an account box */}
        <div className="bg-white py-5 md:border border-[#dbdbdb] rounded-[1px] text-center text-[14px] text-black mb-12">
          Have an account? <Link to="/login" className="font-semibold text-[#0095f6] hover:text-[#1877f2]">Log in</Link>
        </div>

      </div>
    </div>
  );
}
