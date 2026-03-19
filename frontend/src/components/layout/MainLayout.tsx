import React from 'react';
import Sidebar from './Sidebar';

export default function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen bg-white">
      <Sidebar />
      <main className="flex-1 w-full md:ml-64 overflow-y-auto min-h-screen">
        {children}
      </main>
    </div>
  );
}
