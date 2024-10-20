import React, { useEffect, useState } from 'react';
import './Profile.css'; 
import Navbar from "../../components/Navbar/Navbar2";
import Profilepic from "../../assets/profilePic.jpg";
import {jwtDecode} from 'jwt-decode'; 

const Profile = () => {
  const [user, setUser] = useState({
    firstname: '',
    lastname: '',
    email: '',
    identifier: '',
    usertype: '',
  });

  const [isEditing, setIsEditing] = useState(false);
  const [updatedUser, setUpdatedUser] = useState({ ...user });
  const [loading, setLoading] = useState(true);
  
  const [email, setEmail] = useState(null); 
  
  useEffect(() => {
    const token1 = localStorage.getItem('token');
    
    if (token1) {
      const decodedToken = jwtDecode(token1);
      const decodedEmail = decodedToken.sub; 
      setEmail(decodedEmail);
      console.log('Decoded email:', decodedEmail);
    } else {
      console.error("Token not found in localStorage");
    }
  }, []); 

  // Fetch user data with authentication
  useEffect(() => {
    if (email) {
      const fetchProfile = async () => {
        try {
          const response = await fetch(`http://localhost:1200/api/user/profile?email=${email}`, {
            method: "GET",
            headers: {
              authorization: `Bearer ${localStorage.getItem("token")}`, // Pass Bearer token
            },
          });
          
          if (response.ok) {
            const data = await response.json(); // Convert the response to JSON
            setUser(data);
            setUpdatedUser(data);
          } else {
            console.error("Failed to fetch user profile");
          }
        } catch (error) {
          console.error("There was an error fetching the user profile!", error);
        } finally {
          setLoading(false); // End the loading state after fetching
        }
      };
      fetchProfile();
    } else {
      console.error("No email found");
      setLoading(false);
    }
  }, [email]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setUpdatedUser({
      ...updatedUser,
      [name]: value,
    });
  };

  // Update user profile with authentication
  const handleSave = async () => {
    try {
      const response = await fetch(`http://localhost:1200/api/user/profile?email=${email}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          authorization: `Bearer ${localStorage.getItem("token")}`,
        },
        body: JSON.stringify(updatedUser),
      });
  
      if (response.ok) {
        const data = await response.json();
        setUser(data);  // Update the user data in the state
        setIsEditing(false);  // Exit editing mode
      } else {
        const error = await response.text();
        console.error("Error updating profile:", error);
      }
    } catch (error) {
      console.error("Error updating profile:", error);
    }
  };

  if (loading) {
    return <div>Loading...</div>; // Or any loading indicator
  }

  return (
    <>
      <Navbar/>
      <div className='profile-container'>
        <div className='profile-left'>
          <img src={Profilepic} alt="Profile" className="profile-picture"/>
          <div className="profile-name">
            <h2>{user.firstname} {user.lastname}</h2>
          </div>
        </div>

        <div className="profile-right">
        <div className="profile-field">
            <label>Email:</label>
              <p>{user.email}</p>
          </div>

          <div className="profile-field">
            <label>First Name:</label>
            {isEditing ? (
              <input
                type="text"
                name="firstname"
                value={updatedUser.firstname}
                onChange={handleInputChange}
              />
            ) : (
              <p>{user.firstname}</p>
            )}
          </div>

          <div className="profile-field">
            <label>Last Name:</label>
            {isEditing ? (
              <input
                type="text"
                name="lastname"
                value={updatedUser.lastname}
                onChange={handleInputChange}
              />
            ) : (
              <p>{user.lastname}</p>
            )}
          </div>

          

          <div className="profile-field">
            <label>User Type:</label>
            {isEditing ? (
              <input
                type="text"
                name="usertype"
                value={updatedUser.usertype}
                onChange={handleInputChange}
              />
            ) : (
              <p>{user.usertype}</p>
            )}
          </div>

          <div className="profile-field">
            <label>Identifier:</label>
            {isEditing ? (
              <input
                type="text"
                name="identifier"
                value={updatedUser.identifier}
                onChange={handleInputChange}
              />
            ) : (
              <p>{user.identifier}</p>
            )}
          </div>
          
          <div className="button-container">
            {isEditing ? (
              <button className="save-button" onClick={handleSave}>Save</button>
            ) : (
              <button className="edit-button" onClick={() => setIsEditing(true)}>Edit Profile</button>
            )}
          </div>
        </div>
      </div>
    </>
  );
};

export default Profile;
