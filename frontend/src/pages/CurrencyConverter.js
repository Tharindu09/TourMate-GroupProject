import React, { useState, useEffect } from "react";
import Navbar2 from "../components/Navbar/Navbar2";
import Navbar1 from "../components/Navbar/Navbar";
import { useAuth } from "../utils/AuthContext"; // Use the useAuth hook

const CurrencyConverter = () => {
  const [fromCurrency, setFromCurrency] = useState("");
  const [toCurrency, setToCurrency] = useState("");
  const [amount, setAmount] = useState("");
  const [convertedAmount, setConvertedAmount] = useState("");
  const [currencyList, setCurrencyList] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const apiKey = process.env.REACT_APP_CURRENCY_CONVERTER_API_KEY;
    const url = `https://v6.exchangerate-api.com/v6/${apiKey}/latest/USD`;

    

    const fetchCurrencies = async () => {
      try {
        setLoading(true);
        const response = await fetch(url);
        const data = await response.json();

        if (response.ok) {
          setCurrencyList(Object.keys(data.conversion_rates));
        } else {
          throw new Error("Failed to fetch currency list");
        }
      } catch (error) {
        setError(error.message);
      } finally {
        setLoading(false);
      }
    };

    fetchCurrencies();

    const interval = setInterval(fetchCurrencies, 60000); // Fetch every 60 seconds

    return () => clearInterval(interval); // Clear interval on component unmount
  }, []);

  const convertCurrency = async () => {
    try {
      const apiKey = process.env.REACT_APP_CURRENCY_CONVERTER_API_KEY;
      const url = `https://v6.exchangerate-api.com/v6/${apiKey}/pair/${fromCurrency}/${toCurrency}/${amount}`;
      setLoading(true);
      const response = await fetch(url);
      const data = await response.json();

      if (response.ok) {
        if (data.result === "success") {
          setConvertedAmount(data.conversion_result.toFixed(2));
          setError(null);
        } else {
          throw new Error(data.error);
        }
      } else {
        throw new Error("Failed to convert currency");
      }
    } catch (error) {
      setError(error.message);
    } finally {
      setLoading(false);
    }
  };
  const { isAuthenticated } = useAuth();

  return (
    <div  style={{height:'500px' ,paddingTop:'100px'}} >
      {isAuthenticated ? <Navbar2 /> : <Navbar1 />} 
      <div className="container mt-4" >
        <h2 className="mb-4">Currency Converter</h2>
        {error && <p className="text-danger">Error: {error}</p>}
        <div className="row g-3">
          <div className="col-md-4">
            <label htmlFor="fromCurrency" className="form-label">
              From:
            </label>
            <select
              id="fromCurrency"
              className="form-select"
              value={fromCurrency}
              onChange={(e) => setFromCurrency(e.target.value)}
            >
              <option value="">Select Currency</option>
              {currencyList.map((currency) => (
                <option key={currency} value={currency}>
                  {currency}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-4">
            <label htmlFor="toCurrency" className="form-label">
              To:
            </label>
            <select
              id="toCurrency"
              className="form-select"
              value={toCurrency}
              onChange={(e) => setToCurrency(e.target.value)}
            >
              <option value="">Select Currency</option>
              {currencyList.map((currency) => (
                <option key={currency} value={currency}>
                  {currency}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-4">
            <label htmlFor="amount" className="form-label">
              Amount:
            </label>
            <input
              type="number"
              id="amount"
              className="form-control"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </div>
        </div>
        <div className="mt-3">
          <button
            className="btn btn-primary"
            onClick={convertCurrency}
            disabled={!fromCurrency || !toCurrency || !amount || loading}
          >
            {loading ? "Converting..." : "Convert"}
          </button>
        </div>
        {convertedAmount !== "" && (
          <div className="mt-3">
            <h4>
              Converted value is {convertedAmount} {toCurrency}
            </h4>
          </div>
        )}
      </div>
    </div>
  );
};

export default CurrencyConverter;
