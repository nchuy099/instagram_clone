import { Grid, Bookmark, PlaySquare } from 'lucide-react';

export default function ProfileTabs() {
  return (
    <div className="flex flex-col items-center">
      <div className="flex space-x-12 uppercase text-xs font-semibold text-gray-500 tracking-widest">
        <div className="flex items-center space-x-1.5 border-t border-gray-900 pt-4 -mt-[1px] text-gray-900 cursor-pointer">
          <Grid size={14} />
          <span>Posts</span>
        </div>
        <div className="flex items-center space-x-1.5 pt-4 text-gray-500 hover:text-gray-900 cursor-pointer transition">
          <Bookmark size={14} />
          <span className="hidden sm:inline">Saved</span>
        </div>
        <div className="flex items-center space-x-1.5 pt-4 text-gray-500 hover:text-gray-900 cursor-pointer transition">
          <PlaySquare size={14} />
          <span className="hidden sm:inline">Tagged</span>
        </div>
      </div>
      
      <div className="mt-16 text-center text-gray-500 pb-20">
        <div className="w-16 h-16 border-2 border-gray-800 rounded-full flex items-center justify-center mx-auto mb-4">
          <Grid size={32} className="text-gray-800" />
        </div>
        <h2 className="text-2xl font-extrabold text-gray-900 mb-2 mt-6">No Posts Yet</h2>
        <p className="max-w-xs mx-auto text-sm">When they share posts, they will appear on their profile.</p>
      </div>
    </div>
  );
}
