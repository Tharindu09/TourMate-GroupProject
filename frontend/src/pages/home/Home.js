import React from "react";
import './Home.css';
import Navbar from '../../components/Navbar/Navbar';
import Footer from '../../components/Footer/Footer';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faMapMarkedAlt,faUtensils , faHotel} from '@fortawesome/free-solid-svg-icons'; 
import GlobeExchange from '../../assets/Home/GlobeExchange.png';
import RouteFinder from '../../assets/Home/RouteFinder.png';
import SafeLink from '../../assets/Home/SafeLink.png';
import WeatherGuide from '../../assets/Home/WeatherGuide.png';
import { useNavigate, Link } from "react-router-dom";

export default function Home() {
  const navigate = useNavigate();

  const handleFindRoute = () => {
    navigate("/find-route");
  };
  const handleConvertCurrency = () => {
    navigate("/currency-converter");
  };
  const handleEmergencyConnect = () => {
    navigate("/emergency-connector");
  };
  const handleWeather = () => {
    navigate("/weather");
  };

  return (
    <>
            <Navbar/>
            <div className="home-container">
                <header className="home-header">
                    <h1 className='title'>Personalize Your <br/>Travel Schedule</h1>
                    <p className='title-paragraph'>Welcome to Tour Mate, your ultimate travel planning companion! Whether you're embarking on a solo adventure or <br/>
                    planning a family trip, Tour Mate is here to help you create personalized travel schedules with ease. Start by <br/>
                    entering your starting point and destination, and let us take care of the rest.</p>
                    
                    {/* ---------------Three Columns Section------------------- */}
                    <div className="home-container-columns">
                        <div className="home-column">
                            <FontAwesomeIcon icon={faMapMarkedAlt} size="3x" />
                            <h3 style={{ color: 'black' }}>Personalized Tour Planing</h3>
                        </div>
                        <div className="home-column">
                            <FontAwesomeIcon icon={faUtensils} size="3x" />
                            <h3 style={{ color: 'black' }}>Find Restaurants</h3>
                        </div>
                        <div className="home-column">
                            <FontAwesomeIcon icon={faHotel} size="3x" />
                            <h3 style={{ color: 'black' }}>Find Hotels</h3>
                        </div>
                    </div>
                    <div className='login-paragraph'>
                        <p>Welcome back, Traveler! Ready to continue your journey?</p>
                        <Link to="/login">
                        <button className="login-button">Log IN</button></Link>
                        
                    </div>
                </header>
            </div>

            <div className="explore-container">
                    <div className='explore-container-columns'>
                        <div className='explore-column'>
                            <h3>Route Finder</h3>
                            <img src={RouteFinder} alt="Route Finder" className="explore-img"/>
                            <p>Discover the best route between two locations, including your current location, to navigate your journey efficiently.</p>
                            <button className='explore-btn' onClick={handleFindRoute}>Find Route</button>
                        </div>
                        <div className='explore-column'>
                            <h3>Weather Guide</h3>
                            <img src={WeatherGuide} alt="Weather Guide" className="explore-img"/>
                            <p>Get up-to-date weather forecasts for any location to help plan your trips.<br/><br/></p>
                            <button className='explore-btn' onClick={handleWeather}>Find Weather</button>
                        </div>
                        <div className='explore-column'>
                            <h3>Globe Exchange</h3>
                            <img src={GlobeExchange} alt="Globe Exchange" className="explore-img"/>
                            <p>Instantly convert between different currencies using real-time exchange rates.<br/><br/></p>
                            <button className='explore-btn' onClick={handleConvertCurrency}>Convert Currency</button>
                        </div>
                        <div className='explore-column'>
                            <h3>Safe Link</h3>
                            <img src={SafeLink} alt="Safe Link" className="explore-img"/>
                            <p>Quickly access vital emergency contact information, such as police and other services, to ensure safety during your travels.</p>
                            <button className='explore-btn' onClick={handleEmergencyConnect}>Emergency Connector</button>
                        </div>
                        
                    </div><Footer/>  
                </div>
            
        </>
  );
}
