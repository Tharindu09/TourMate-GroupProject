CREATE TABLE places (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, destination_id BIGINT NOT NULL,
 name VARCHAR(160) NOT NULL, type VARCHAR(20) NOT NULL, description TEXT NOT NULL,
 address VARCHAR(500) NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL,
 average_rating DECIMAL(3,2) NOT NULL DEFAULT 0, review_count INT NOT NULL DEFAULT 0,
 price_level INT NOT NULL DEFAULT 0, estimated_cost DECIMAL(12,2) NOT NULL DEFAULT 0,
 phone VARCHAR(40), website VARCHAR(1000), cover_image_url VARCHAR(1000),
 CONSTRAINT fk_place_destination FOREIGN KEY(destination_id) REFERENCES destinations(id),
 CONSTRAINT ck_place_type CHECK(type IN ('HOTEL','RESTAURANT','ATTRACTION','ACTIVITY')),
 CONSTRAINT ck_place_rating CHECK(average_rating BETWEEN 0 AND 5),
 CONSTRAINT ck_place_reviews CHECK(review_count >= 0),
 CONSTRAINT ck_place_price CHECK(price_level BETWEEN 0 AND 4 AND estimated_cost >= 0),
 INDEX idx_place_discovery(destination_id, type, estimated_cost)
);
CREATE TABLE hotel_detail (
 place_id BIGINT PRIMARY KEY, star_rating INT, check_in_time TIME, check_out_time TIME,
 FOREIGN KEY(place_id) REFERENCES places(id) ON DELETE CASCADE
);
CREATE TABLE restaurant_detail (
 place_id BIGINT PRIMARY KEY, cuisine VARCHAR(160), vegetarian_options BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY(place_id) REFERENCES places(id) ON DELETE CASCADE
);
CREATE TABLE attraction_detail (
 place_id BIGINT PRIMARY KEY, suggested_duration_minutes INT, difficulty VARCHAR(20),
 FOREIGN KEY(place_id) REFERENCES places(id) ON DELETE CASCADE
);
CREATE TABLE place_photo (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, place_id BIGINT NOT NULL, url VARCHAR(1000) NOT NULL, caption VARCHAR(255),
 FOREIGN KEY(place_id) REFERENCES places(id) ON DELETE CASCADE
);
CREATE TABLE amenity (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE);
CREATE TABLE place_amenity (
 place_id BIGINT NOT NULL, amenity_id BIGINT NOT NULL, PRIMARY KEY(place_id, amenity_id),
 FOREIGN KEY(place_id) REFERENCES places(id) ON DELETE CASCADE, FOREIGN KEY(amenity_id) REFERENCES amenity(id)
);
CREATE TABLE place_opening_hours (
 place_id BIGINT NOT NULL, day_of_week INT NOT NULL, opens_at TIME, closes_at TIME, closed BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(place_id, day_of_week), FOREIGN KEY(place_id) REFERENCES places(id) ON DELETE CASCADE,
 CHECK(day_of_week BETWEEN 1 AND 7)
);
