import React from 'react';
import { useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';

export default function MainLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const isMessagesRoute = location.pathname.startsWith('/messages');

  return (
    <div className={`min-h-screen bg-white ${isMessagesRoute ? 'md:pl-20' : 'md:pl-64'}`}>
      <Sidebar />
      <main className="min-h-screen overflow-y-auto">
        {children}
      </main>
    </div>
  );
}
