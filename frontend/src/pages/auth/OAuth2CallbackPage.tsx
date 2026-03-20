import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

export default function OAuth2CallbackPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const token = params.get('token');
    const refreshToken = params.get('refreshToken');
    const isUsernameSetParam = params.get('isUsernameSet');
    const isUsernameSet = isUsernameSetParam === 'true';
    const error = params.get('error');

    console.log('OAuth2 Callback Data:', { token: !!token, isUsernameSetParam, isUsernameSet });

    if (token) {
      login({ accessToken: token, refreshToken: refreshToken || '' });
      if (isUsernameSetParam === 'false') {
        navigate('/setup-username', { replace: true });
      } else {
        navigate('/', { replace: true });
      }
    } else {
      console.error('OAuth2 callback error:', error);
      navigate('/login', { replace: true, state: { error: 'Failed to log in with Facebook.' } });
    }
  }, [location, login, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <p>Authenticating...</p>
    </div>
  );
}
