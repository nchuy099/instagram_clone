import { Home, Search, Compass, MessageCircle, Heart, PlusSquare, User, Menu } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export default function Sidebar() {
  const { user, logout } = useAuth();

  return (
    <div className="hidden md:flex flex-col w-64 border-r border-gray-300 px-3 py-6 h-full fixed bg-white z-10">
      <div className="mb-10 px-4">
        <h1 className="text-2xl font-bold font-serif italic cursor-pointer">Instagram</h1>
      </div>

      <nav className="flex-1 space-y-2">
        <Link to="/" className="flex items-center space-x-4 p-3 hover:bg-gray-100 rounded-lg transition">
          <Home size={24} />
          <span className="text-[15px] font-semibold">Home</span>
        </Link>
        <div className="flex items-center space-x-4 p-3 hover:bg-gray-100 rounded-lg transition cursor-pointer">
          <Search size={24} />
          <span className="text-[15px]">Search</span>
        </div>
        <div className="flex items-center space-x-4 p-3 hover:bg-gray-100 rounded-lg transition cursor-pointer">
          <Compass size={24} />
          <span className="text-[15px]">Explore</span>
        </div>
        <div className="flex items-center space-x-4 p-3 hover:bg-gray-100 rounded-lg transition cursor-pointer">
          <MessageCircle size={24} />
          <span className="text-[15px]">Messages</span>
        </div>
        <div className="flex items-center space-x-4 p-3 hover:bg-gray-100 rounded-lg transition cursor-pointer">
          <Heart size={24} />
          <span className="text-[15px]">Notifications</span>
        </div>
        <div className="flex items-center space-x-4 p-3 hover:bg-gray-100 rounded-lg transition cursor-pointer">
          <PlusSquare size={24} />
          <span className="text-[15px]">Create</span>
        </div>
        <Link to="/profile/me" className="flex items-center space-x-4 p-3 hover:bg-gray-100 rounded-lg transition">
          <div className="w-6 h-6 rounded-full bg-gray-300 overflow-hidden">
            {user?.avatarUrl ? <img src={user.avatarUrl} alt="avatar" /> : <User size={24} className="text-gray-500" />}
          </div>
          <span className="text-[15px] font-semibold">Profile</span>
        </Link>
      </nav>

      <div className="mt-auto">
        <button onClick={logout} className="flex items-center space-x-4 p-3 w-full hover:bg-gray-100 rounded-lg text-left transition text-red-500">
          <Menu size={24} />
          <span className="text-[15px]">Log out</span>
        </button>
      </div>
    </div>
  );
}
