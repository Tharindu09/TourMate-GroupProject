import axios from "axios";

export const getPlaceData = async (sw, ne, type) => {
  const options = {
    params: {
      bl_latitude: sw.lat,
      tr_latitude: ne.lat,
      bl_longitude: sw.lng,
      tr_longitude: ne.lng,
      // open_now: "false",
    },
    headers: {
      "x-rapidapi-key": "9f81c80342msh99a1c2dc4870077p1ad952jsn95227fa78b79",
      "x-rapidapi-host": "travel-advisor.p.rapidapi.com",
    },
  };
  try {
    const {
      data: { data },
    } = await axios.get(
      `https://travel-advisor.p.rapidapi.com/${type}/list-in-boundary`,
      options
    );
    return data;
  } catch (error) {
    console.error(error);
  }
};

export const getHotelData = async (
  sw,
  ne,
  checkinDate,
  checkoutDate,
  rooms,
  adults,
  children
) => {
  const options = {
    params: {
      neLat: ne.lat,
      neLng: ne.lng,
      swLat: sw.lat,
      swLng: sw.lng,
      checkinDate: checkinDate,
      checkoutDate: checkoutDate,
      rooms: rooms,
      adults: adults,
      units: "metric",
    },
    headers: {
      "x-rapidapi-key": "9f81c80342msh99a1c2dc4870077p1ad952jsn95227fa78b79",
      "x-rapidapi-host": "booking-com18.p.rapidapi.com",
    },
  };
  if (children > 0) {
    options.params.children = Array(children).fill(10).join(",");
  }
  try {
    const {
      data: {
        data: { results },
      },
    } = await axios.get(
      "https://booking-com18.p.rapidapi.com/stays/search-by-geo",
      options
    );
    // console.log(results);
    return results;
  } catch (error) {
    console.error(error);
  }
};

export const fetchExchangeRate = async () => {
  try {
    const response = await axios.get(
      "https://v6.exchangerate-api.com/v6/6018d6bca6c0633fc54f6a62/pair/USD/LKR"
    );

    const rate = response.data.conversion_rate;
    return rate; // Set the exchange rate in state
  } catch (error) {
    console.error("Error fetching the exchange rate:", error);
  }
};

export const getPlaceSuggestions = async (query) => {
  const options = {
    params: {
      input: query,
      radius: "500",
    },
    headers: {
      "x-rapidapi-key": "8ecbf3f5d9mshdae784e4c5b86a1p1e8241jsneaae86936521",
      "x-rapidapi-host": "place-autocomplete1.p.rapidapi.com",
    },
  };
  try {
    const response = await axios.get(
      "https://place-autocomplete1.p.rapidapi.com/autocomplete/json",
      options
    );
    console.log(response);

    if (response.status !== 200) {
      console.log("Error fetching place suggestions");
      return [];
    }

    if (response.data.predictions.length === 0) {
      console.log("No predictions found");
      return [];
    }

    return response.data.predictions;
  } catch (error) {
    console.error(error);
  }
};
