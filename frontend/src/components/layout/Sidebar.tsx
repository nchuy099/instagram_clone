import { useState } from 'react';
import { Home, Search, Compass, MessageCircle, Heart, PlusSquare, User, Menu } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import CreatePostModal from '../../features/post/components/CreatePostModal';

type NavItemProps = {
  to?: string;
  icon: React.ReactNode;
  label: string;
  collapsed: boolean;
  onClick?: () => void;
};

function NavItem({ to, icon, label, collapsed, onClick }: NavItemProps) {
  const className = `flex items-center ${collapsed ? 'justify-center' : 'space-x-4'} p-3 hover:bg-gray-100 rounded-lg transition`;

  if (to) {
    return (
      <Link to={to} className={className}>
        {icon}
        {!collapsed ? <span className="text-[15px]">{label}</span> : null}
      </Link>
    );
  }

  return (
    <button type="button" className={`${className} w-full text-left cursor-pointer`} onClick={onClick}>
      {icon}
      {!collapsed ? <span className="text-[15px]">{label}</span> : null}
    </button>
  );
}

export default function Sidebar() {
  const location = useLocation();
  const isMessagesRoute = location.pathname.startsWith('/messages');

  const { user, logout } = useAuth();
  const [isModalOpen, setIsModalOpen] = useState(false);

  const sidebarWidthClass = isMessagesRoute ? 'w-20 px-2' : 'w-64 px-3';

  return (
    <>
      <div className={`hidden md:flex flex-col border-r border-gray-300 py-6 h-full fixed bg-white z-10 ${sidebarWidthClass}`}>
        <div className={`mb-8 ${isMessagesRoute ? 'px-0 flex justify-center' : 'px-4'}`}>
          {isMessagesRoute ? (
            <h1 className="text-2xl font-bold font-serif italic cursor-pointer">I</h1>
          ) : (
            <h1 className="text-2xl font-bold font-serif italic cursor-pointer">Instagram</h1>
          )}
        </div>

        <nav className="flex-1 space-y-2">
          <NavItem to="/" icon={<Home size={24} />} label="Home" collapsed={isMessagesRoute} />
          <NavItem to="/search" icon={<Search size={24} />} label="Search" collapsed={isMessagesRoute} />
          <NavItem to="/explore" icon={<Compass size={24} />} label="Explore" collapsed={isMessagesRoute} />
          <NavItem to="/messages" icon={<MessageCircle size={24} />} label="Messages" collapsed={isMessagesRoute} />
          <NavItem icon={<Heart size={24} />} label="Notifications" collapsed={isMessagesRoute} />
          <NavItem
            icon={<PlusSquare size={24} />}
            label="Create"
            collapsed={isMessagesRoute}
            onClick={() => setIsModalOpen(true)}
          />

          <Link
            to="/me"
            className={`flex items-center ${isMessagesRoute ? 'justify-center' : 'space-x-4'} p-3 hover:bg-gray-100 rounded-lg transition`}
          >
            <div className="w-6 h-6 rounded-full bg-gray-300 overflow-hidden">
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />
              ) : (
                <User size={24} className="text-gray-500" />
              )}
            </div>
            {!isMessagesRoute ? <span className="text-[15px] font-semibold">Profile</span> : null}
          </Link>
        </nav>

        <div className="mt-auto">
          <button
            onClick={logout}
            className={`flex items-center ${isMessagesRoute ? 'justify-center' : 'space-x-4'} p-3 w-full hover:bg-gray-100 rounded-lg text-left transition text-red-500`}
          >
            <Menu size={24} />
            {!isMessagesRoute ? <span className="text-[15px]">Log out</span> : null}
          </button>
        </div>
      </div>

      <CreatePostModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={() => {
          console.log('Post created successfully!');
        }}
      />
    </>
  );
}
