import { FiBookmark, FiTag, FiGrid } from 'react-icons/fi';

interface ProfileTabsProps {
  activeTab: 'posts' | 'saved' | 'tagged';
  onTabChange: (tab: 'posts' | 'saved' | 'tagged') => void;
  showSaved?: boolean;
}

export default function ProfileTabs({ activeTab, onTabChange, showSaved = false }: ProfileTabsProps) {
  return (
    <div className="flex flex-col items-center">
      <div className="flex space-x-12 uppercase text-xs font-semibold tracking-widest border-t border-gray-300 w-full justify-center">
        <div 
          onClick={() => onTabChange('posts')}
          className={`flex items-center space-x-1.5 pt-4 -mt-[1px] cursor-pointer transition ${
            activeTab === 'posts' ? 'border-t border-gray-900 text-gray-900' : 'text-gray-500 hover:text-gray-900'
          }`}
        >
          <FiGrid size={14} />
          <span>Posts</span>
        </div>
        
        {showSaved && (
          <div 
            onClick={() => onTabChange('saved')}
            className={`flex items-center space-x-1.5 pt-4 -mt-[1px] cursor-pointer transition ${
              activeTab === 'saved' ? 'border-t border-gray-900 text-gray-900' : 'text-gray-500 hover:text-gray-900'
            }`}
          >
            <FiBookmark size={14} />
            <span className="hidden sm:inline">Saved</span>
          </div>
        )}

        <div 
          onClick={() => onTabChange('tagged')}
          className={`flex items-center space-x-1.5 pt-4 -mt-[1px] cursor-pointer transition ${
            activeTab === 'tagged' ? 'border-t border-gray-900 text-gray-900' : 'text-gray-500 hover:text-gray-900'
          }`}
        >
          <FiTag size={14} />
          <span className="hidden sm:inline">Tagged</span>
        </div>
      </div>
    </div>
  );
}
