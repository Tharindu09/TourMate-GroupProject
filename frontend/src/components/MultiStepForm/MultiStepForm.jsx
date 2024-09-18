import { useState } from "react";

const ProgressBar = ({ progress }) => (
  <div style={{ width: "100%", backgroundColor: "#e0e0df" }}>
    <div
      style={{
        width: `${progress}%`,
        backgroundColor: "#76c7c0",
        height: "10px",
      }}
    />
  </div>
);

const MultiStepForm = () => {
  const [step, setStep] = useState(1);
  const [userPlan, setUserPlan] = useState({
    startLocation: "",
    endLocation: "",
    startTime: "",
    endTime: "",
    userPreference: "",
  });

  // Handling next and back steps
  const nextStep = () => {
    if (step < 3) setStep(step + 1);
  };

  const prevStep = () => {
    if (step > 1) setStep(step - 1);
  };

  // Handling input changes
  const handleChange = (input) => (e) => {
    setUserPlan({ ...userPlan, [input]: e.target.value });
  };

  // Progress bar percentage based on step
  const getProgress = () => (step / 3) * 100;

  return (
    <div>
      <ProgressBar progress={getProgress()} />
      {step === 1 && <Step1 handleChange={handleChange} userPlan={userPlan} />}
      {step === 2 && <Step2 handleChange={handleChange} userPlan={userPlan} />}
      {step === 3 && <Step3 handleChange={handleChange} userPlan={userPlan} />}

      <div>
        <button onClick={prevStep} disabled={step === 1}>
          Back
        </button>
        <button onClick={nextStep}>{step === 3 ? "Submit" : "Next"}</button>
      </div>
    </div>
  );
};

const Step1 = ({ handleChange, userPlan }) => (
  <div>
    <label>Start Location:</label>
    <input
      type="text"
      value={userPlan.startLocation}
      onChange={handleChange("startLocation")}
    />

    <label>End Location:</label>
    <input
      type="text"
      value={userPlan.endLocation}
      onChange={handleChange("endLocation")}
    />
  </div>
);

const Step2 = ({ handleChange, userPlan }) => (
  <div>
    <label>Start Time:</label>
    <input
      type="time"
      value={userPlan.startTime}
      onChange={handleChange("startTime")}
    />

    <label>End Time:</label>
    <input
      type="time"
      value={userPlan.endTime}
      onChange={handleChange("endTime")}
    />
  </div>
);

const Step3 = ({ handleChange, userPlan }) => (
  <div>
    <label>User Preference:</label>
    <input
      type="text"
      value={userPlan.userPreference}
      onChange={handleChange("userPreference")}
    />
  </div>
);

export default MultiStepForm;
