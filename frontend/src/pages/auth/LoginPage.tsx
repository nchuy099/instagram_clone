import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { FaFacebook } from 'react-icons/fa6';
import { useAuth } from '../../hooks/useAuth';
import api from '../../lib/axios';

export default function LoginPage() {
  const [emailOrUsername, setEmailOrUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrors([]);

    if (!emailOrUsername.trim() || !password.trim()) {
      setErrors(['Please fill in all fields.']);
      setIsLoading(false);
      return;
    }

    try {
      const response = await api.post('/auth/login', { identifier: emailOrUsername, password });
      await login(response.data.data);
      navigate('/');
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      const data = err.response?.data;
      const errorDetails = data?.error?.details || data?.details;

      if (errorDetails && typeof errorDetails === 'object') {
        const detailMsgs = Object.values(errorDetails) as string[];
        setErrors(detailMsgs);
      } else {
        const errorMsg = data?.error?.message || data?.message || 'Sorry, your password was incorrect. Please double-check your password.';
        setErrors([errorMsg]);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleFacebookLogin = () => {
    window.location.href = 'http://localhost:8080/oauth2/authorization/facebook';
  };


  return (
    <div className="min-h-screen flex flex-col justify-center items-center bg-white md:bg-[#fafafa] py-12 px-4 selection:bg-blue-100 font-sans text-black">
      <div className="w-full max-w-[350px] flex flex-col gap-2.5">

        {/* Logo and Form Box */}
        <div className="bg-white px-10 pt-10 pb-5 md:border border-[#dbdbdb] rounded-[1px] flex flex-col items-center">
          <div className="relative mb-8 flex w-full items-center justify-center">
            <img src="/insta_logo.png" alt="Instagram logo" className="absolute left-2 h-[52px] w-[52px] object-contain" />
            <span className="font-['Grand_Hotel'] text-[48px] leading-none text-black">Instagram</span>
          </div>

          <form onSubmit={handleSubmit} className="w-full flex flex-col gap-[6px]">
            <input
              id="emailOrUsername"
              type="text"
              required
              autoComplete="username"
              className="w-full px-2.5 py-[9px] border border-[#dbdbdb] rounded-[3px] bg-[#fafafa] text-[12px] text-black placeholder-[#8e8e8e] focus:outline-none focus:border-[#a8a8a8]"
              placeholder="Phone number, username, or email"
              value={emailOrUsername}
              onChange={(e) => setEmailOrUsername(e.target.value)}
            />
            <div className="relative">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                required
                autoComplete="current-password"
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

            <button
              type="submit"
              disabled={isLoading}
              className={`w-full mt-2 py-[5px] text-white text-[14px] font-semibold rounded-[8px] transition-colors bg-[#0095f6] hover:bg-[#1877f2] ${isLoading ? 'opacity-70 cursor-default' : ''
                }`}
            >
              {isLoading ? 'Logging in...' : 'Log in'}
            </button>

            {errors.length > 0 && (
              <div className="mt-4 flex flex-col gap-1">
                {errors.map((err, index) => (
                  <p key={index} className="text-[#ed4956] text-[14px] text-center">{err}</p>
                ))}
              </div>
            )}

            {/* OR Divider */}
            <div className="flex items-center w-full my-4">
              <div className="flex-grow border-t border-[#dbdbdb]"></div>
              <span className="px-4 text-[13px] font-semibold text-[#8e8e8e]">OR</span>
              <div className="flex-grow border-t border-[#dbdbdb]"></div>
            </div>

            {/* Facebook Login */}
            <button
              type="button"
              onClick={handleFacebookLogin}
              className="flex items-center justify-center gap-2 mx-auto hover:opacity-70 transition-opacity"
            >
              <FaFacebook className="text-[#385185]" size={14} />
              <span className="text-[14px] font-semibold text-[#385185]">Log in with Facebook</span>
            </button>



            {/* Forgot password */}
            <div className="text-center mt-3">
              <Link to="/forgot-password" title="Forgot password" className="text-[12px] text-[#00376b] hover:underline">
                Forgot password?
              </Link>
            </div>
          </form>
        </div>

        {/* Sign up box */}
        <div className="bg-white py-5 md:border border-[#dbdbdb] rounded-[1px] text-center text-[14px] text-black">
          Don't have an account? <Link to="/register" className="font-semibold text-[#0095f6] hover:text-[#1877f2]">Sign up</Link>
        </div>

        {/* Footer (Simplified) */}
        <div className="flex flex-wrap justify-center gap-x-4 gap-y-2 px-4 opacity-70">
          {['Meta', 'About', 'Blog', 'Jobs', 'Help', 'API', 'Privacy', 'Terms', 'Locations', 'Instagram Lite', 'Threads', 'Contact Uploading & Non-Users', 'Meta Verified'].map(item => (
            <span key={item} className="text-[12px] text-[#8e8e8e] cursor-pointer hover:underline">{item}</span>
          ))}
        </div>
        <div className="text-center mt-4 opacity-70">
          <p className="text-[12px] text-[#8e8e8e]">English © 2024 Instagram from Meta</p>
        </div>

      </div>
    </div>
  );
}
