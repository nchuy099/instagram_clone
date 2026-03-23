import { useState, useEffect } from 'react';
import { X, User } from 'lucide-react';
import { Link } from 'react-router-dom';
import api from '../../../lib/axios';

interface UserListModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  username: string;
  type: 'followers' | 'following';
}

export default function UserListModal({ isOpen, onClose, title, username, type }: UserListModalProps) {
  const [users, setUsers] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (isOpen && username) {
      setIsLoading(true);
      api.get(`/users/${username}/${type}`)
        .then(res => setUsers(res.data.data.content))
        .catch(err => console.error(err))
        .finally(() => setIsLoading(false));
    }
  }, [isOpen, username, type]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="bg-white w-full max-w-[400px] rounded-xl overflow-hidden flex flex-col max-h-[400px]">
        <div className="flex items-center justify-between p-4 border-b border-gray-200">
          <div className="w-8"></div>
          <h3 className="font-bold text-base">{title}</h3>
          <button onClick={onClose} className="p-1 hover:bg-gray-100 rounded-full">
            <X size={20} />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 p-4">
          {isLoading ? (
            <div className="text-center text-gray-500 py-10">Loading...</div>
          ) : users.length === 0 ? (
            <div className="text-center text-gray-500 py-10">No {type} yet.</div>
          ) : (
            <div className="space-y-4">
              {users.map((user: any) => (
                <div key={user.id} className="flex items-center justify-between">
                  <Link to={`/${user.username}`} onClick={onClose} className="flex items-center flex-1">
                    <div className="w-11 h-11 rounded-full overflow-hidden bg-gray-100 flex-shrink-0">
                      {user.avatarUrl ? (
                        <img src={user.avatarUrl} alt="avatar" className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center bg-gray-200">
                          <User size={20} className="text-gray-400" />
                        </div>
                      )}
                    </div>
                    <div className="ml-3">
                      <p className="font-bold text-sm text-gray-900">{user.username}</p>
                      <p className="text-gray-500 text-sm">{user.fullName}</p>
                    </div>
                  </Link>
                  {/* Option to follow/unfollow could go here */}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
