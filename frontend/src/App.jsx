import "./App.css";
import Login from "./pages/Login";
import Signup from "./pages/Signup";
import Home from "./pages/home/Home";
import Plan from "./pages/plan/Plan";
import RoutePage from "./pages/RoutePage";
import SearchPlace from "./pages/SearchPlace";
import AddBookmarks from "./pages/bookmarks/AddBookmarks";
import Dashboard from "./pages/dashboard/Dashboard";
import CurrencyConverter from "./pages/CurrencyConverter";
import EmergencyConnector from "./pages/EmergencyConnector";
import Weather from "./pages/Weather";
import HotelPage from "./pages/Hotel/HotelPage";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./utils/AuthContext";
import ProtectedRoutes from "./utils/ProtectedRoutes";
import { Box, Container } from "@mui/material";
import Profile from "./pages/ProfilePage/Profile.jsx";
import ForgotPassword from "./pages/ForgotPassword";
import SchedulePlan from "./pages/schedulePlanning/SchedulePlan";
import { useAuth } from "./utils/AuthContext";
import Footer from "./components/Footer/Footer";

const App = () => {
  const { isAuthenticated } = useAuth();
  return (
    <AuthProvider>
      <Router>
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            minHeight: "100vh",
            alignItems: "center",
          }}
        >
          <Container
            component="main"
            sx={{
              flex: 1,
            }}
            style={{
              padding: 0,
              margin: 0,
            }}
          >
            <div>
              <Routes>
                <Route element={<ProtectedRoutes />}>
                  {/* Add other protected routes here if needed */}=
                  <Route path="/profile" element={<Profile />} />
                  <Route path="/add-bookmarks" element={<AddBookmarks />} />
                  <Route path="/create-plan" element={<Plan />} />
                  <Route path="/find-hotel" element={<HotelPage />} />
                  <Route path="/find-places" element={<SearchPlace />} />
                  <Route path="/find-restaurants" element={<SearchPlace />} />
                  <Route path="/schedule-plan" element={<SchedulePlan />} />
                  <Route path="/dashboard" element={<Dashboard />} />
                </Route>
                <Route path="/" element={<Home />} />
                <Route path="/signup" element={<Signup />} />
                <Route path="/login" element={<Login />} />
                <Route path="/find-route" element={<RoutePage />} />
                <Route
                  path="/currency-converter"
                  element={<CurrencyConverter />}
                />
                <Route
                  path="/emergency-connector"
                  element={<EmergencyConnector />}
                />
                <Route path="/weather" element={<Weather />} />
                <Route path="/forgot-password" element={<ForgotPassword />} />
              </Routes>
            </div>
            <Footer />
          </Container>
        </Box>
      </Router>
    </AuthProvider>
  );
};

export default App;
