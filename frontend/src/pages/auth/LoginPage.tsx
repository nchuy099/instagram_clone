import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import api from '../../lib/axios';

export default function LoginPage() {
  const [emailOrUsername, setEmailOrUsername] = useState('');
  const [password, setPassword] = useState('');
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
      const errorMsg = err.response?.data?.error?.message || err.response?.data?.message || 'Failed to login';
      setError(errorMsg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900 font-serif italic">Mini Instagram</h2>
      </div>

      <div className="mt-4 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-none sm:px-10 border border-gray-300 mx-4 sm:mx-0">
          <form className="space-y-4" onSubmit={handleSubmit}>
            {error && <div className="text-red-500 text-sm text-center">{error}</div>}
            <div>
              <input
                id="emailOrUsername"
                type="text"
                required
                className="appearance-none block w-full px-2 py-2 border border-gray-300 rounded-sm bg-gray-50 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-1 focus:ring-gray-400 focus:border-gray-400 sm:text-sm"
                placeholder="Phone number, username, or email"
                value={emailOrUsername}
                onChange={(e) => setEmailOrUsername(e.target.value)}
              />
            </div>
            <div>
              <input
                id="password"
                type="password"
                required
                className="appearance-none block w-full px-2 py-2 border border-gray-300 rounded-sm bg-gray-50 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-1 focus:ring-gray-400 focus:border-gray-400 sm:text-sm"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={isLoading || !emailOrUsername || !password}
                className="w-full flex justify-center py-1.5 px-4 border border-transparent text-sm font-semibold rounded-lg text-white bg-[#0095f6] hover:bg-[#1877f2] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-70 disabled:cursor-not-allowed"
              >
                {isLoading ? 'Logging in...' : 'Log in'}
              </button>
            </div>
          </form>
        </div>
        
        <div className="mt-4 bg-white py-4 px-4 shadow sm:rounded-none sm:px-10 border border-gray-300 text-center text-sm text-gray-900 mx-4 sm:mx-0">
          Don't have an account? <Link to="/register" className="font-semibold text-[#0095f6] hover:text-blue-800">Sign up</Link>
        </div>
      </div>
    </div>
  );
}
