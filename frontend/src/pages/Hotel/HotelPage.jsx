import {
  Box,
  Button,
  Grid,
  FormControl,
  MenuItem,
  InputLabel,
  Select,
  Slider,
  Typography,
  CircularProgress,
  Popover,
  Checkbox,
  FormControlLabel,
} from "@mui/material";
import React, { useState, useEffect } from "react";
import PlaceSuggest from "../../components/PlaceSuggest/PlaceSuggest";
import { getHotelData } from "../../api";
import HotelCard from "../../components/SearchHeader/HotelCard/HotelCard";
import { useCallback } from "react";
import { debounce } from "lodash";
import { fetchExchangeRate } from "../../api";
import { KeyboardArrowDown } from "@mui/icons-material";
import "./Hotel.css";

export default function HotelPage() {
  const today = new Date().toISOString().split("T")[0];

  const [coordinates, setCoordinates] = useState(null);
  const [checkInDate, setCheckInDate] = useState(today);
  const [checkOutDate, setCheckOutDate] = useState("");
  const [hotels, setHotels] = useState([]);
  const [exchangeRate, setExchangeRate] = useState(null);
  const [score, setScore] = useState();
  const [filteredHotels, setFilteredHotels] = useState([]);
  const [priceRange, setPriceRange] = useState([0, 100]);
  const [minPrice, setMinPrice] = useState(null);
  const [maxPrice, setMaxPrice] = useState(null);
  const [numOfNights, setNumOfNights] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [adults, setAdults] = useState(1);
  const [rooms, setRooms] = useState(1);
  const [children, setChildren] = useState(0);
  const [anchorEl, setAnchorEl] = useState(null);
  const [cancelChecked, setCancelChecked] = useState(false);
  const [prePayChecked, setPrePayChecked] = useState(false);

  const handleCheck = (event) => {
    setCancelChecked(event.target.checked);
  };

  const handleCheckPrePay = (event) => {
    setPrePayChecked(event.target.checked);
  };

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const open = Boolean(anchorEl);
  const id = open ? "simple-popover" : undefined;

  const handleChange = (event, newValue) => {
    setPriceRange(newValue);
  };

  const getNumOfNights = () => {
    if (checkInDate && checkOutDate) {
      // Calculate the difference in time
      const checkIn = new Date(checkInDate);
      const checkOut = new Date(checkOutDate);

      // Difference in milliseconds
      const timeDiff = checkOut.getTime() - checkIn.getTime();

      // Convert milliseconds to days (1 day = 24 * 60 * 60 * 1000)
      const nights = timeDiff / (1000 * 60 * 60 * 24);

      // Only update if the difference is positive (valid date range)
      if (nights > 0) {
        setNumOfNights(nights);
      } else {
        setNumOfNights(0);
      }
    }
  };
  // Debounce to trigger state update after the user stops moving the slider
  const debouncedHandleChange = useCallback(debounce(handleChange, 100), []);

  useEffect(() => {
    // Function to fetch the exchange rate from USD to LKR
    fetchExchangeRate().then((rate) => {
      setExchangeRate(rate);
    });
  }, []);

  const createBound = (coordinates) => {
    const centerLat = parseFloat(coordinates.lat);
    const centerLng = parseFloat(coordinates.long);

    // Define a distance in degrees for latitude and longitude
    // Note: Adjust the distance as needed
    const latOffset = 0.1; // Latitude offset
    const lngOffset = 0.1; // Longitude offset

    // Calculate the southwest (SW) and northeast (NE) corners
    const swLat = centerLat - latOffset;
    const swLng = centerLng - lngOffset;
    const neLat = centerLat + latOffset;
    const neLng = centerLng + lngOffset;

    // Define the bounds
    const bounds = {
      sw: { lat: swLat, lng: swLng },
      ne: { lat: neLat, lng: neLng },
    };
    return bounds;
  };

  const handleSearch = () => {
    setIsLoading(true);
    const bounds = createBound(coordinates);
    getHotelData(
      bounds.sw,
      bounds.ne,
      checkInDate,
      checkOutDate,
      rooms,
      adults,
      children
    ).then((data) => {
      console.log(data);
      setHotels(data);
      setFilteredHotels([]);
      getNumOfNights();
      setScore(0);
      setIsLoading(false);
    });
  };

  useEffect(() => {
    if (hotels.length > 0) {
      const prices = hotels.map((hotel) =>
        (
          hotel.priceDisplayInfo.displayPrice.amountPerStay.amountUnformatted *
          exchangeRate
        ).toFixed(0)
      );
      const min = Math.min(...prices);
      const max = Math.max(...prices);
      const roundedMin = Math.floor(min / 1000) * 1000;
      const roundedMax = Math.ceil(max / 1000) * 1000;
      setMinPrice(roundedMin);
      setMaxPrice(roundedMax);
      setPriceRange([roundedMin, roundedMax]);
    }
  }, [hotels, exchangeRate]);

  useEffect(() => {
    const filterHotels = hotels.filter((hotel) => {
      const hotelScore = hotel.basicPropertyData.reviews.totalScore;
      const hotelPrice =
        hotel.priceDisplayInfo.displayPrice.amountPerStay.amountUnformatted *
        exchangeRate;
      const prePayCheck = hotel.policies.showNoPrepayment;
      const freeCancel = hotel.policies.showFreeCancellation;

      // Check if the hotel's score, price, prepayment, and cancellation policies match
      return (
        hotelScore >= score &&
        hotelPrice >= priceRange[0] &&
        hotelPrice <= priceRange[1] &&
        (!prePayChecked || prePayCheck) && // Filter by prepayment if prePayChecked is true
        (!cancelChecked || freeCancel) // Filter by free cancellation if cancelChecked is true
      );
    });

    setFilteredHotels(filterHotels);
  }, [score, priceRange, hotels, exchangeRate, prePayChecked, cancelChecked]);
  return (
    <>
      <Box
        display="flex"
        alignItems="center"
        justifyContent="space-around"
        className="simple-header"
        sx={{
          height: "50px",
          position: "sticky",
          top: 0,
          left: 0,
          width: "100%",
          zIndex: 1000,
          backgroundColor: "#fff",
        }}
      >
        <PlaceSuggest setCoordinates={setCoordinates}></PlaceSuggest>
        <div>
          {/* <label for="checkIn">Check-In Date:</label> */}
          <input
            type="date"
            id="checkIn"
            name="checkIn"
            onChange={(e) => setCheckInDate(e.target.value)}
            value={checkInDate}
            min={today}
            placeholder="check-in"
          />
        </div>

        <div>
          {/* <label for="checkout">Check-Out Date:</label> */}
          <input
            type="date"
            id="checkout"
            name="checkout"
            onChange={(e) => setCheckOutDate(e.target.value)}
            min={checkInDate || today}
            placeholder="check-out"
          />
        </div>
        <div>
          {/* Single Dropdown Button */}
          <Button
            variant="outlined"
            onClick={handleClick}
            style={{ color: "grey" }} // Changing text color to grey
          >
            {adults > 0 && `${adults} adults`}
            {children > 0 && ` .${children} children`}
            {rooms > 0 && ` .${rooms} rooms`} <KeyboardArrowDown />
          </Button>

          {/* Popover Container */}
          <Popover
            id={id}
            open={open}
            anchorEl={anchorEl}
            onClose={handleClose}
            anchorOrigin={{
              vertical: "bottom",
              horizontal: "left",
            }}
          >
            <Box p={2}>
              <Grid container spacing={2}>
                {/* Adults Dropdown */}
                <Grid item xs={12}>
                  <FormControl fullWidth>
                    <InputLabel>Adults</InputLabel>
                    <Select
                      value={adults}
                      onChange={(event) => {
                        setAdults(event.target.value);
                      }}
                    >
                      {[...Array(10).keys()].map((value) => (
                        <MenuItem key={value + 1} value={value + 1}>
                          {value + 1} Adult{value + 1 > 1 ? "s" : ""}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                {/* Rooms Dropdown */}
                <Grid item xs={12}>
                  <FormControl fullWidth>
                    <InputLabel>Rooms</InputLabel>
                    <Select
                      value={rooms}
                      onChange={(event) => {
                        setRooms(event.target.value);
                      }}
                    >
                      {[...Array(5).keys()].map((value) => (
                        <MenuItem key={value + 1} value={value + 1}>
                          {value + 1} Room{value + 1 > 1 ? "s" : ""}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                {/* Children Dropdown */}
                <Grid item xs={12}>
                  <FormControl fullWidth>
                    <InputLabel>Children</InputLabel>
                    <Select
                      value={children}
                      onChange={(event) => {
                        setChildren(event.target.value);
                      }}
                    >
                      {[...Array(10).keys()].map((value) => (
                        <MenuItem key={value} value={value}>
                          {value} Child{value > 1 ? "ren" : ""}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
              </Grid>
            </Box>
          </Popover>
        </div>
        <Button variant="contained" onClick={handleSearch}>
          Search
        </Button>
      </Box>
      <>
        {isLoading ? (
          <div className="loading">
            <CircularProgress size="5rem" />
          </div>
        ) : (
          hotels.length > 0 && (
            <Grid
              container
              spacing={3}
              style={{ width: "100%", padding: "10px" }}
            >
              <Grid
                item
                xs={2}
                md={4}
                sx={{
                  position: "fixed",
                  top: "100px",
                  left: "15vh",
                  width: "30%", // Sidebar width
                  height: "100vh", // Full viewport height
                  padding: "10px",
                }}
              >
                <FormControl
                  className="formControl"
                  sx={{ width: "50%", marginBottom: "40px" }}
                >
                  <InputLabel>Score</InputLabel>
                  <Select
                    value={score}
                    className="select"
                    onChange={(e) => setScore(e.target.value)}
                  >
                    <MenuItem value={6}>Pleasant 6+</MenuItem>
                    <MenuItem value={7}>Good 7+</MenuItem>
                    <MenuItem value={8}>Very Good 8+</MenuItem>
                    <MenuItem value={9}>Superb 9+</MenuItem>
                  </Select>
                </FormControl>
                <FormControl
                  className="formControl"
                  sx={{ width: "100%", height: "150px" }}
                >
                  <Box>
                    <Typography gutterBottom>Price Range</Typography>
                    <Slider
                      value={priceRange}
                      onChange={debouncedHandleChange}
                      valueLabelDisplay="auto"
                      min={minPrice}
                      max={maxPrice}
                      step={5000}
                      aria-labelledby="range-slider"
                      sx={{ width: "60%" }}
                    />
                    <Box
                      sx={{
                        display: "flex",
                        justifyContent: "space-between",
                        mt: 2,
                        width: "60%",
                      }}
                    >
                      <Typography>Min: LKR {priceRange[0]}</Typography>
                      <Typography>Max: LKR {priceRange[1]}</Typography>
                    </Box>
                  </Box>
                </FormControl>
                <FormControl>
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={cancelChecked}
                        onChange={handleCheck}
                        color="primary"
                      />
                    }
                    label="Free Cancelation"
                  />
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={prePayChecked}
                        onChange={handleCheckPrePay}
                        color="primary"
                      />
                    }
                    label="No Prepayment"
                  />
                </FormControl>
              </Grid>
              <Grid
                item
                xs={10}
                md={8}
                sx={{
                  marginLeft: "35%",
                  overflowY: "auto",
                }}
              >
                {(filteredHotels.length ? filteredHotels : hotels)?.map(
                  (hotel, i) => (
                    <HotelCard
                      key={i}
                      hotel={hotel}
                      exchangeRate={exchangeRate}
                      nights={numOfNights}
                      adults={adults}
                      children={children}
                    />
                  )
                )}
              </Grid>
            </Grid>
          )
        )}
      </>
    </>
  );
}
