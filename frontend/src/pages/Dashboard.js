import React from "react";
import "bootstrap/dist/css/bootstrap.min.css";
import { useNavigate } from "react-router-dom";
import Header from "../components/Header";

export default function Dashboard() {
  const navigate = useNavigate();

  const handleFindRoute = () => {
    navigate("/find-route");
  };

  const handleFindHotel = () => {
    navigate("/find-hotel");
  };

  const handleFindRestaurant = () => {
    navigate("/find-restaurant");
  };

  const handleSchedulePlan = () => {
    navigate("/schedule-plan");
  };

  const handleBookmarks = () => {
    navigate("/add-bookmarks");
  };

  const handlePlan = () => {
    navigate("/create-plan");
  };

  const btnStyle = {
    backgroundColor: "#007bff",
    color: "#ffffff",
    border: "none",
    padding: "10px 20px",
    cursor: "pointer",
    borderRadius: "4px",
    marginRight: "16px", // Adjust the margin here
  };
  return (
    <div>
      <div className="container mt-5">
        <div className="jumbotron">
          <h1 className="display-4">Welcome to Tourmate</h1>
          <p className="lead">
            Tourmate is your ultimate travel companion, designed to enhance your
            travel experience and make your journeys unforgettable. Whether you
            are planning a solo adventure or a group trip, Tourmate provides you
            with the tools you need to plan, organize, and enjoy your travels.
          </p>
          <hr className="my-4" />
          <p>
            With Tourmate, you can easily discover new destinations, create
            itineraries, find travel buddies, and share your experiences with
            friends and fellow travelers. Our user-friendly platform ensures
            that you have all the information and resources you need at your
            fingertips.
          </p>
          <ul>
            <li>Discover exciting destinations and hidden gems</li>
            <li>Create and manage your travel itineraries</li>
            <li>Connect with fellow travelers and find travel buddies</li>
            <li>Share your travel experiences and memories</li>
            <li>Get personalized recommendations and travel tips</li>
          </ul>
          <div>
            {
              <>
                <button style={btnStyle} onClick={handleFindRoute}>
                  Find a Route
                </button>
                <button style={btnStyle} onClick={handleFindHotel}>
                  Find a Hotel
                </button>
                <button style={btnStyle} onClick={handleFindRestaurant}>
                  Find a Places
                </button>
                <button style={btnStyle} onClick={handleSchedulePlan}>
                  Schedule a Plan
                </button>
                <button style={btnStyle} onClick={handleBookmarks}>
                  Add Bookmarks
                </button>
                <button style={btnStyle} onClick={handlePlan}>
                  Create Plan
                </button>
              </>
            }
          </div>
        </div>
      </div>
    </div>
  );
}
