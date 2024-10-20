import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../utils/AuthContext";
import {
  Avatar,
  FormControlLabel,
  Grid,
  Paper,
  TextField,
  Link,
  Typography,
  InputAdornment,
} from "@mui/material";
import LockOutlinedIcon from "@mui/icons-material/LockOutlined";
import Checkbox from "@mui/material/Checkbox";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";

export default function Logtest() {
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false); // State for "Remember me" checkbox
  const navigate = useNavigate(); // For navigation after successful login
  const [errorMessage, setErrorMessage] = useState("");

  // Load remember me state from localStorage on component mount
  useEffect(() => {
    const rememberMeChecked = localStorage.getItem("rememberMe") === "true";
    setRememberMe(rememberMeChecked);
    if (rememberMeChecked) {
      const savedEmail = localStorage.getItem("email") || "";
      setEmail(savedEmail);
    }
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();

    try {
      console.log("Sending login request to backend...");
      const response = await fetch("http://localhost:1200/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email, password }),
      });

      if (response.ok) {
        const jwtToken = await response.text(); // Read the JWT token from the response body
        console.log("Login successful, received JWT token:", jwtToken);
        // Store the token securely, e.g., in localStorage or session storage
        login(jwtToken); // Update the auth context with the JWT token

        // Save email and rememberMe state to localStorage if rememberMe is checked
        if (rememberMe) {
          localStorage.setItem("email", email);
          localStorage.setItem("rememberMe", true);
        } else {
          localStorage.removeItem("email");
          localStorage.removeItem("rememberMe");
        }

        // Navigate to Dashboard
        console.log("Navigating to Dashboard...");
        navigate("/Dashboard");
      } else {
        // Login failed, handle error (e.g., show error message)
        console.error("Login failed with status:", response.status);

        setErrorMessage("Incorrect email or password. Please try again.");
      }
    } catch (error) {
      console.error("Error during login:", error);
      setErrorMessage("An error occurred during login. Please try again.");
    }
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };
  const handleRememberMe = (event) => {
    setRememberMe(event.target.checked);
  };

  const paperstyle = {
    padding: 20,
    height: "70vh",
    width: 300,
    margin: "20px auto",
  };
  const avatarStyle = { backgroundColor: "#1bbd7e" };
  const btstyle = { margin: "8px 0" };
  return (
    <div>
      <form onSubmit={handleSubmit}>
        <Grid>
          <Paper elevation={10} style={paperstyle}>
            <Grid align="center">
              <Avatar style={avatarStyle}>
                <LockOutlinedIcon />
              </Avatar>
              <h2>Log in</h2>
            </Grid>
            <TextField
              label="Email"
              style={{ margin: "8px 0" }}
              placeholder="Enter email"
              fullWidth
              required
              id="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <TextField
              label="Password"
              style={{ margin: "8px 0" }}
              placeholder="Enter password"
              type={showPassword ? "text" : "password"}
              fullWidth
              required
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                  <IconButton
                    aria-label="toggle password visibility"
                    onClick={togglePasswordVisibility}
                    edge="end"
                  >
                    {showPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                  </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            <FormControlLabel
              control={
                <Checkbox
                  name="checkedB"
                  color="primary"
                  checked={rememberMe}
                  onChange={handleRememberMe}
                />
              }
              label="Remember me"
            />
            {errorMessage && <p style={{ color: "red" }}>{errorMessage}</p>}
            <Button
              type="submit"
              color="primary"
              variant="contained"
              style={btstyle}
              fullWidth
            >
              Log in
            </Button>
            <Typography>
              <Link href="ForgotPassword" variant="plain">
                Forgot password?
              </Link>
            </Typography>
            <Typography style={{ margin: "75px 0" }}>
              Do you haven't an account?
              <Link href="Signup" variant="plain">
                Sign Up
              </Link>
            </Typography>
          </Paper>
        </Grid>
      </form>
    </div>
  );
}
