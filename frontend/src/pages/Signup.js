import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  Box,
  Button,
  Card,
  FormHelperText,
  Grid,
  InputAdornment,
  Paper,
  TextField,
  Typography,
} from "@mui/material";
import IconButton from "@mui/material/IconButton";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import SampleImage1 from "../assets/images/signup.jpg";
const paperStyle = {
  padding: 20,
  height: "100%",
  width: "100%",
  margin: "20px auto",
};
const textstyle = {
  variance: "h5",
  color: "black", // or any other color you prefer
  fontWeight: "bold",
  textAlign: "center",
  marginBottom: 2, // adds some spacing below the text
};
export default function Signup() {



  const navigate = useNavigate();

  const initialFormData = {
    firstname: "",
    lastname: "",
    email: "",
    gender: "",
    age: "",
    usertype: "local",
    identifier: "",
    password: "",
  };

  const [formData, setFormData] = useState(initialFormData);

  const handleInputChange = (event) => {
    const { name, value } = event.target;

    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    // Log form data to the console
    console.log("Form Data to be submitted:", formData);

    try {
      const response = await fetch("http://localhost:1200/auth/signup", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      // Log the raw response
      console.log("Raw response:", response);

      // Check if response is not empty
      if (response.ok) {
        const responseData = await response.json();
        console.log("Response from the backend:", responseData);

        if (responseData) {
          console.log("Form Data Submitted Successfully");
          alert("Form is submitted");
          navigate("/Dashboard");
          setFormData(initialFormData);
        } else {
          console.error(
            "Failed to submit form data:",
            response.status,
            responseData
          );
          alert(
            `Failed to submit form data: ${response.status} - ${responseData}`
          );
        }
      } else {
        console.error("Failed to submit form data:", response.status);
        alert(`Failed to submit form data: ${response.status}`);
      }
    } catch (error) {
      console.error("Error submitting form data:", error);
      alert("Error submitting form data");
    }
  };
  const [showPassword, setShowPassword] = useState(false);
  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  return (
    <div>
      <Grid align="center">
        <Grid container spacing={2} item xs={12} md={8}>
          <Grid item xs={12} md={8} align="left">
            <Paper elevation={10} style={paperStyle}>
              <Typography
                variant="h5"
                sx={{
                  color: "primary.main", // or any other color you prefer
                  fontWeight: "bold",
                  textAlign: "center",
                  marginBottom: 2, // adds some spacing below the text
                }}
              >
                Sign Up
              </Typography>
              <Box
                component="hr"
                sx={{
                  border: 0,
                  height: "2px",
                  backgroundColor: "primary.main",
                  margin: "20px 0",
                }}
              />
              <Typography>
                <form onSubmit={handleSubmit}>
                  <div>
                    <label htmlFor="firstname">First Name</label>
                    <br />
                    <TextField
                      type="text"
                      name="firstname"
                      placeholder="Firstname"
                      value={formData.firstname}
                      onChange={handleInputChange}
                      required
                      fullWidth
                      InputProps={{
                        style: {
                          height: "40px",
                        },
                      }}
                    />
                  </div>
                  <div>
                    <label htmlFor="lastname">Last Name</label>
                    <br />
                    <TextField
                      type="text"
                      name="lastname"
                      placeholder="Lastname"
                      value={formData.lastname}
                      onChange={handleInputChange}
                      fullWidth
                      InputProps={{
                        style: {
                          height: "40px",
                        },
                      }}
                    />
                  </div>
                  <div>
                    <label htmlFor="email">Email</label>
                    <br />
                    <TextField
                      type="email"
                      name="email"
                      placeholder="example@ex.com"
                      value={formData.email}
                      onChange={handleInputChange}
                      required
                      fullWidth
                      InputProps={{
                        style: {
                          height: "40px",
                        },
                      }}
                    />
                  </div>
                  <div>
                    <label htmlFor="gender">Gender</label>
                    <br />
                    <select
                      name="gender"
                      value={formData.gender}
                      onChange={handleInputChange}
                      required
                    >
                      <option value="">Select your gender</option>
                      <option value="male">Male</option>
                      <option value="female">Female</option>
                      <option value="other">Other</option>
                      <option value="prefer_not_to_say">
                        Prefer not to say
                      </option>
                    </select>
                  </div>
                  <div>
                    <label htmlFor="age">Age</label>
                    <br />
                    <TextField
                      type="number"
                      name="age"
                      placeholder="Age"
                      value={formData.age}
                      onChange={handleInputChange}
                      required
                      fullWidth
                      InputProps={{
                        style: {
                          height: "40px",
                        },
                      }}
                    />
                  </div>

                  <div>
                    <label htmlFor="password">Password</label>
                    <br />
                    <TextField
                      type={showPassword ? "text" : "password"}
                      name="password"
                      placeholder="Password"
                      value={formData.password}
                      onChange={handleInputChange}
                      required
                      fullWidth
                      InputProps={{
                        style: {
                          height: "40px",
                        },
                        endAdornment: (
                          <InputAdornment position="end">
                          <IconButton
                            aria-label="toggle password visibility"
                            onClick={togglePasswordVisibility}
                            edge="end"
                          >
                            {showPassword ? (
                              <VisibilityOffIcon />
                            ) : (
                              <VisibilityIcon />
                            )}
                          </IconButton>
                        </InputAdornment>
                        ),
                      }}
                    />
                    <FormHelperText>Enter a strong password</FormHelperText>
                  </div>
                  <div>
                    <label htmlFor="usertype">
                      Are you a local or a foreigner?
                    </label>
                    <br />
                    <select
                      name="usertype"
                      value={formData.usertype}
                      onChange={handleInputChange}
                    >
                      <option value="local">Local</option>
                      <option value="foreigner">Foreigner</option>
                    </select>
                  </div>
                  {formData.usertype === "local" ? (
                    <div>
                      <label htmlFor="nic">NIC Number</label>
                      <br />
                      <TextField
                        type="text"
                        name="identifier"
                        placeholder="NIC"
                        value={formData.identifier}
                        onChange={handleInputChange}
                        required
                        fullWidth
                        InputProps={{
                          style: {
                            height: "40px",
                          },
                        }}
                      />
                    </div>
                  ) : (
                    <div>
                      <label htmlFor="passport">Passport Number</label>
                      <br />
                      <TextField
                        type="text"
                        name="identifier"
                        placeholder="Passport"
                        value={formData.identifier}
                        onChange={handleInputChange}
                        required
                        fullWidth
                        InputProps={{
                          style: {
                            height: "40px",
                          },
                        }}
                      />
                    </div>
                  )}


                  <Button
                    type="submit"
                    color="primary"
                    variant="contained"
                    style={{ margin: "50px 0" }}
                    fullWidth
                  >
                    SIGN UP
                  </Button>
                  <p>
                    Already have an account? <Link to="/login">Log in</Link>
                  </p>
                </form>
              </Typography>
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper elevation={10} style={paperStyle}>
              <Grid container direction="column" alignItems="center">
                <Typography sx={textstyle}>Welcome to Tour Mate</Typography>
                <img
                  src={SampleImage1}
                  alt="Sample"
                  style={{ width: "100%", height: "auto", margin: "20px 0" }}
                />
                <Typography>
                  Tour Mate helps you schedule your travels efficiently. Whether
                  you're exploring new destinations or revisiting your favorite
                  spots, our app ensures you have a well-organized itinerary
                  tailored to your preferences. Sign up today and start planning
                  your perfect trip!
                </Typography>
              </Grid>
              <Box
                component="hr"
                sx={{
                  border: 0,
                  height: "2px",
                  backgroundColor: "#000",
                  margin: "20px 0",
                }}
              />

              <Card variant="outlined" sx={{ maxWidth: 400 }}>
                <Typography sx={textstyle}>Features and benifits</Typography>
                <Typography>
                  01.Route recommendation:
                  <br />
                  <br />
                  <p>
                    {" "}
                    We suggest the most suitable route schedule for day-to-day
                    users (including hotels, restaurants, etc.) We plan to be
                    GPS based and show directions to points of interest
                  </p>
                  02.Search and Filters:
                  <br />
                  <br />
                  <p>
                    {" "}
                    Users can search for places or activities based on their
                    preference Allow users to search for specific places or
                    activities and use filters to refine their search
                  </p>
                </Typography>
              </Card>
            </Paper>
          </Grid>
        </Grid>
      </Grid>
    </div>
  );
}
