import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../../lib/axios';

export default function RegisterPage() {
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');
    
    try {
      await api.post('/auth/register', { email, fullName, username, password });
      navigate('/login');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900 font-serif italic mb-4">Mini Instagram</h2>
      </div>

      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-none border border-gray-300 sm:px-10 mx-4 sm:mx-0">
          <p className="text-center text-md text-gray-500 font-semibold mb-6">Sign up to see photos and videos from your friends.</p>
          
          <form className="space-y-4" onSubmit={handleSubmit}>
            {error && <div className="text-red-500 text-sm text-center">{error}</div>}
            <div>
              <input
                id="email"
                type="email"
                required
                className="appearance-none block w-full px-2 py-2 border border-gray-300 rounded-sm bg-gray-50 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-1 focus:ring-gray-400 focus:border-gray-400 sm:text-sm"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div>
              <input
                id="fullName"
                type="text"
                className="appearance-none block w-full px-2 py-2 border border-gray-300 rounded-sm bg-gray-50 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-1 focus:ring-gray-400 focus:border-gray-400 sm:text-sm"
                placeholder="Full Name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
            </div>
            <div>
              <input
                id="username"
                type="text"
                required
                className="appearance-none block w-full px-2 py-2 border border-gray-300 rounded-sm bg-gray-50 placeholder-gray-500 text-gray-900 focus:outline-none focus:ring-1 focus:ring-gray-400 focus:border-gray-400 sm:text-sm"
                placeholder="Username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
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
                disabled={isLoading || !email || !username || !password}
                className="w-full flex justify-center py-1.5 px-4 border border-transparent text-sm font-semibold rounded-lg text-white bg-[#0095f6] hover:bg-[#1877f2] focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-70 disabled:cursor-not-allowed"
              >
                {isLoading ? 'Signing up...' : 'Sign up'}
              </button>
            </div>
          </form>
        </div>
        
        <div className="mt-4 bg-white py-4 px-4 shadow sm:rounded-none border border-gray-300 sm:px-10 text-center text-sm text-gray-900 mx-4 sm:mx-0">
          Have an account? <Link to="/login" className="font-semibold text-[#0095f6] hover:text-blue-800">Log in</Link>
        </div>
      </div>
    </div>
  );
}
