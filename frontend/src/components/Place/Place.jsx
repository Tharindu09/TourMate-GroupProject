import React, { useState } from "react";
import {
  LocationOn,
  Phone,
  BookmarkTwoTone as BookmarkIcon,
  BookmarkBorder as BookmarkBorderIcon,
} from "@mui/icons-material";
import "./Place.css";
import {
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CardMedia,
  Chip,
  Rating,
  Typography,
  useMediaQuery,
  Tooltip,
  IconButton,
} from "@mui/material";

import { useAuth } from "../../utils/AuthContext";

export default function Place({
  place,
  selected,
  refProp,
  setCardSelect,
  index,
}) {
  const isDesktop = useMediaQuery("(min-width:600px)");
  const [Bookmark, setBookmark] = useState(false);
  const { isAuthenticated } = useAuth();

  const handleBookmark = () => {
    setBookmark((prevBookmark) => !prevBookmark);
  };

  if (selected) {
    refProp?.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    selected = false;
  }

  return (
    <Card elevation={6} className="card">
      <CardMedia
        style={{ height: 350 }}
        image={
          place.photo
            ? place.photo.images.large.url
            : "https://st4.depositphotos.com/14953852/24787/v/1600/depositphotos_247872612-stock-illustration-no-image-available-icon-vector.jpg"
        }
        title={place.name}
        onClick={() => setCardSelect(index)}
        className="card-media"
      />
      <CardContent>
        <Typography variant="h5" gutterBottom>
          {place.name}
        </Typography>
        <Box display="flex" justifyContent="space-between">
          <Rating value={Number(place.rating)} precision={0.5} readOnly />
          <Typography gutterBottom variant="subtitle">
            out of {place.num_reviews} reviews
          </Typography>
        </Box>
        {place?.price && (
          <Box display="flex" justifyContent="space-between">
            <Typography variant="subtitle1">Price</Typography>
            <Typography gutterBottom variant="subtitle">
              {place.price}
            </Typography>
          </Box>
        )}
        <Box display="flex" justifyContent="space-between">
          <Typography variant="subtitle" marginRight="30px">
            Ranking
          </Typography>
          <Typography gutterBottom variant="subtitle">
            {place.ranking}
          </Typography>
        </Box>
        {place?.cuisine?.map(({ name }) => (
          <Chip key={name} size="small" label={name} className="chip"></Chip>
        ))}
        {place?.subtype?.map(({ name }) => (
          <Chip key={name} size="small" label={name} className="chip"></Chip>
        ))}
        {place?.address && (
          <Typography
            gutterBottom
            variant="subtitle2"
            color="textSecondary"
            className="subtitle"
          >
            <LocationOn />
            {place.address}
          </Typography>
        )}
        {place?.phone && (
          <Typography
            gutterBottom
            variant="subtitle2"
            color="textSecondary"
            className="spacing"
          >
            <Phone />
            {isDesktop ? (
              <span className="phoneLink">{place.phone}</span>
            ) : (
              <a href={`tel:${place.phone}`} className="phoneLink">
                {place.phone}
              </a>
            )}
          </Typography>
        )}
        <CardActions
          sx={{
            display: "flex",
            justifyContent: "space-between",
          }}
        >
          <Button
            size="small"
            color="primary"
            onClick={() => window.open(place.web_url, "_blank")}
          >
            Trip Advisor
          </Button>
          {place?.website && (
            <Button
              size="small"
              color="primary"
              onClick={() => window.open(place.website, "_blank")}
            >
              Website
            </Button>
          )}
          {isAuthenticated && place.category.key === "attraction" && (
            <Tooltip title="Add bookmark">
              <IconButton
                onClick={handleBookmark}
                style={{ cursor: "pointer" }}
              >
                {Bookmark ? (
                  <BookmarkIcon
                    sx={{
                      color: "green",
                    }}
                  />
                ) : (
                  <BookmarkBorderIcon />
                )}
              </IconButton>
            </Tooltip>
          )}
        </CardActions>
      </CardContent>
    </Card>
  );
}
