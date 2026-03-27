import React from 'react';
import { useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';

export default function MainLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const isMessagesRoute = location.pathname.startsWith('/messages');

  return (
    <div className="flex min-h-screen bg-white">
      <Sidebar />
      <main className={`flex-1 w-full overflow-y-auto min-h-screen ${isMessagesRoute ? 'md:ml-20' : 'md:ml-64'}`}>
        {children}
      </main>
    </div>
  );
}
