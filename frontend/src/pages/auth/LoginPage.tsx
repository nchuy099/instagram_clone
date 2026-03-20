import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import api from '../../lib/axios';

export default function LoginPage() {
  const [emailOrUsername, setEmailOrUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    try {
      const response = await api.post('/auth/login', { emailOrUsername, password });
      login(response.data.data);
      navigate('/');
    } catch (err: any) {
      const errorMsg = err.response?.data?.error?.message || err.response?.data?.message || 'Sorry, your password was incorrect. Please double-check your password.';
      setError(errorMsg);
    } finally {
      setIsLoading(false);
    }
  };

  const isFormValid = emailOrUsername.length > 0 && password.length > 0;

  return (
    <div className="min-h-screen flex flex-col justify-center items-center bg-white md:bg-[#fafafa] py-12 px-4 selection:bg-blue-100 font-sans text-black">
      <div className="w-full max-w-[350px] flex flex-col gap-2.5">

        {/* Logo and Form Box */}
        <div className="bg-white px-10 pt-10 pb-5 md:border border-[#dbdbdb] rounded-[1px] flex flex-col items-center">
          <h1 className="text-[32px] font-bold text-black mb-8 tracking-tighter">Instagram</h1>
          
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
              disabled={isLoading || !isFormValid}
              className={`w-full mt-2 py-[5px] text-white text-[14px] font-semibold rounded-[8px] transition-colors ${
                isFormValid ? 'bg-[#0095f6] hover:bg-[#1877f2]' : 'bg-[#b2dffc] cursor-default'
              }`}
            >
              {isLoading ? 'Logging in...' : 'Log in'}
            </button>

            {/* OR Divider */}
            <div className="flex items-center w-full my-4">
              <div className="flex-grow border-t border-[#dbdbdb]"></div>
              <span className="px-4 text-[13px] font-semibold text-[#8e8e8e]">OR</span>
              <div className="flex-grow border-t border-[#dbdbdb]"></div>
            </div>

            {/* Facebook Login */}
            <button
              type="button"
              className="flex items-center justify-center gap-2 mx-auto hover:opacity-70 transition-opacity"
            >
              <svg className="w-4 h-4 text-[#385185] fill-current" viewBox="0 0 24 24">
                <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.469h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.469h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
              </svg>
              <span className="text-[14px] font-semibold text-[#385185]">Log in with Facebook</span>
            </button>

            {error && (
              <p className="text-[#ed4956] text-[14px] text-center mt-4">{error}</p>
            )}

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
