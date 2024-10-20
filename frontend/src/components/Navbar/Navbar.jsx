import React from "react";
import "./Navbar.css";
import logob from "../../assets/logob.png";
import { Link } from "react-router-dom";
const Navbar = () => {
  //   useEffect(() => {
  //     const handleScroll = () => {
  //       const header = document.querySelector(".header");
  //       const logo = document.querySelector(".logo");
  //       const navLinks = document.querySelectorAll(".nav a");

  //       if (window.scrollY > 50) {
  //         // Adjust the scroll value as needed
  //         header.classList.add("scrolled");
  //         logo.classList.add("scrolled");
  //         navLinks.forEach((link) => link.classList.add("scrolled"));
  //       } else {
  //         header.classList.remove("scrolled");
  //         logo.classList.remove("scrolled");
  //         navLinks.forEach((link) => link.classList.remove("scrolled"));
  //       }
  //     };

  //     window.addEventListener("scroll", handleScroll);

  //     return () => {
  //       window.removeEventListener("scroll", handleScroll);
  //     };
  //   }, []);

  return (
    <header className="nav-header">
      <a href="/" className="nav-logo">
        <img src={logob} alt="Tour Mate Logo" className="logo-image" />
      </a>

      <nav className="nav">
        <Link to="/">Home</Link>
        <Link to="/">Tours</Link>
        <Link to="/">About me</Link>
        <Link to="/">Contact me</Link>
      </nav>
      <Link to="/signup" className="link-button">
        <button className="signin-btn">SIGN IN</button>
      </Link>
    </header>
  );
};

export default Navbar;
