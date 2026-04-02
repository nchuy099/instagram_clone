import { useState } from 'react';
import {
  FaInstagram,
} from 'react-icons/fa6';
import {
  FiCompass,
  FiHeart,
  FiHome,
  FiMenu,
  FiMessageCircle,
  FiPlusSquare,
  FiSearch,
  FiUser,
} from 'react-icons/fi';
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
  const [isMoreOpen, setIsMoreOpen] = useState(false);
  const [isLogoutConfirmOpen, setIsLogoutConfirmOpen] = useState(false);

  const sidebarWidthClass = isMessagesRoute ? 'w-20 px-2' : 'w-64 px-3';

  return (
    <>
      <div className={`fixed inset-y-0 left-0 z-10 hidden h-screen flex-col border-r border-gray-300 bg-white py-6 md:flex ${sidebarWidthClass}`}>
        <div className={`mb-8 ${isMessagesRoute ? 'px-0 flex justify-center' : 'px-4'}`}>
          {isMessagesRoute ? (
            <FaInstagram size={28} className="cursor-pointer" />
          ) : (
            <div className="flex items-center gap-2">
              <FaInstagram size={28} />
              <span className="text-2xl font-bold font-serif italic cursor-pointer">Instagram</span>
            </div>
          )}
        </div>

        <nav className="flex-1 space-y-2">
          <NavItem to="/" icon={<FiHome size={22} />} label="Home" collapsed={isMessagesRoute} />
          <NavItem to="/search" icon={<FiSearch size={22} />} label="Search" collapsed={isMessagesRoute} />
          <NavItem to="/explore" icon={<FiCompass size={22} />} label="Explore" collapsed={isMessagesRoute} />
          <NavItem to="/messages" icon={<FiMessageCircle size={22} />} label="Messages" collapsed={isMessagesRoute} />
          <NavItem icon={<FiHeart size={22} />} label="Notifications" collapsed={isMessagesRoute} />
          <NavItem
            icon={<FiPlusSquare size={22} />}
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
                <FiUser size={20} className="text-gray-500" />
              )}
            </div>
            {!isMessagesRoute ? <span className="text-[15px] font-semibold">Profile</span> : null}
          </Link>
        </nav>

        <div className="mt-auto pb-1">
          <button
            onClick={() => setIsMoreOpen((prev) => !prev)}
            className={`flex items-center ${isMessagesRoute ? 'justify-center' : 'space-x-4'} p-3 w-full hover:bg-gray-100 rounded-lg text-left transition`}
          >
            <FiMenu size={22} />
            {!isMessagesRoute ? <span className="text-[15px]">More</span> : null}
          </button>

          {isMoreOpen ? (
            <div className={`absolute bottom-16 ${isMessagesRoute ? 'left-2 right-2' : 'left-3 right-3'} rounded-xl border border-gray-200 bg-white p-2 shadow-lg`}>
              <button
                type="button"
                className="w-full rounded-lg px-3 py-2 text-left text-sm text-red-500 transition hover:bg-gray-50"
                onClick={() => {
                  setIsMoreOpen(false);
                  setIsLogoutConfirmOpen(true);
                }}
              >
                Log out
              </button>
            </div>
          ) : null}
        </div>
      </div>

      <CreatePostModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={() => {
          console.log('Post created successfully!');
        }}
      />

      {isLogoutConfirmOpen ? (
        <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/50 p-4">
          <div className="w-full max-w-sm overflow-hidden rounded-xl bg-white shadow-xl">
            <div className="border-b border-gray-200 px-5 py-4">
              <h3 className="text-base font-semibold text-gray-900">Log out?</h3>
              <p className="mt-1 text-sm text-gray-500">You will need to log in again to continue.</p>
            </div>
            <div className="flex items-center justify-end gap-2 px-5 py-4">
              <button
                type="button"
                className="rounded-lg px-4 py-2 text-sm text-gray-600 transition hover:bg-gray-100"
                onClick={() => setIsLogoutConfirmOpen(false)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="rounded-lg bg-red-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-600"
                onClick={() => {
                  setIsLogoutConfirmOpen(false);
                  void logout();
                }}
              >
                Log out
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
