import type { ReactNode } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './features/auth/AuthContext';
import { useAuth } from './hooks/useAuth';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import OAuth2CallbackPage from './pages/auth/OAuth2CallbackPage';
import SetupUsernamePage from './pages/auth/SetupUsernamePage';
import ProfilePage from './pages/profile/ProfilePage';
import EditProfilePage from './pages/accounts/EditProfilePage';

const ProtectedRoute = ({ children }: { children: ReactNode }) => {
  const { isAuthenticated, isLoading } = useAuth();
  
  if (isLoading) return <div className="min-h-screen flex items-center justify-center">Loading...</div>;
  if (!isAuthenticated) return <Navigate to="/login" />;
  
  return children;
};

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
          <Route path="/setup-username" element={<SetupUsernamePage />} />
          <Route 
            path="/accounts/edit" 
            element={
              <ProtectedRoute>
                <EditProfilePage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/:username" 
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            } 
          />
          {/* Default route redirects to personal profile if authenticated, or falls back to protected route block */}
          <Route path="/" element={
            <ProtectedRoute>
              <Navigate to="/me" />
            </ProtectedRoute>
          } />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
