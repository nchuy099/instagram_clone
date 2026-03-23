import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../lib/axios';
import { useAuth } from '../../hooks/useAuth';

export default function SetupUsernamePage() {
  const [username, setUsername] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { setUser } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      // axios baseURL has /api already
      const resp = await api.patch('/users/me/username', { username });
      setUser(resp.data.data);
      navigate('/');
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update username');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#fafafa]">
      <div className="bg-white p-10 border border-[#dbdbdb] rounded w-full max-w-[350px]">
        <h1 className="text-2xl font-bold text-center mb-6">Choose a username</h1>
        <p className="text-sm text-gray-500 text-center mb-6">
          You can change this later, but for now, please choose a unique username for your profile.
        </p>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <input
            type="text"
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="w-full bg-[#fafafa] border border-[#dbdbdb] rounded-[3px] p-2 text-[12px] focus:outline-none focus:border-[#a8a8a8]"
            required
          />
          <button
            type="submit"
            disabled={isLoading || !username}
            className="bg-[#0095f6] text-white rounded p-1.5 text-[14px] font-semibold disabled:opacity-50"
          >
            {isLoading ? 'Updating...' : 'Set Username'}
          </button>
        </form>
        {error && <p className="text-red-500 text-xs mt-4 text-center">{error}</p>}
      </div>
    </div>
  );
}
