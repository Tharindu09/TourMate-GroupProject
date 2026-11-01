CREATE TABLE trips (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, name VARCHAR(160) NOT NULL,
 start_date DATE NOT NULL, end_date DATE NOT NULL, budget DECIMAL(12,2) NOT NULL,
 travelers INT NOT NULL, planning_mode VARCHAR(20) NOT NULL DEFAULT 'MANUAL', status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
 CHECK(end_date >= start_date), CHECK(budget >= 0), CHECK(travelers BETWEEN 1 AND 100),
 INDEX idx_trip_owner(user_id, updated_at)
);
CREATE TABLE trip_destinations (
 trip_id BIGINT NOT NULL, destination_id BIGINT NOT NULL, PRIMARY KEY(trip_id,destination_id),
 FOREIGN KEY(trip_id) REFERENCES trips(id) ON DELETE CASCADE,
 FOREIGN KEY(destination_id) REFERENCES destinations(id)
);
CREATE TABLE trip_days (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, trip_id BIGINT NOT NULL, day_number INT NOT NULL, date DATE NOT NULL,
 FOREIGN KEY(trip_id) REFERENCES trips(id) ON DELETE CASCADE,
 UNIQUE KEY uq_trip_date(trip_id,date), CHECK(day_number >= 1)
);
CREATE TABLE trip_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, day_id BIGINT NOT NULL, title VARCHAR(300) NOT NULL,
 category VARCHAR(20) NOT NULL, place_source VARCHAR(20) NOT NULL, place_id VARCHAR(2000), address VARCHAR(1000),
 latitude DOUBLE, longitude DOUBLE, start_time TIME NOT NULL, end_time TIME NOT NULL,
 estimated_cost DECIMAL(12,2) NOT NULL, notes VARCHAR(4000) NOT NULL, sort_order INT NOT NULL,
 FOREIGN KEY(day_id) REFERENCES trip_days(id) ON DELETE CASCADE,
 CHECK(end_time > start_time), CHECK(estimated_cost >= 0), CHECK(sort_order >= 0),
 CHECK((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL AND latitude BETWEEN 5.8 AND 9.95 AND longitude BETWEEN 79.4 AND 82)),
 CHECK(category IN ('HOTEL','RESTAURANT','ATTRACTION','ACTIVITY','TRANSPORT','OTHER')),
 CHECK(place_source IN ('CUSTOM','TOURMATE','GEOAPIFY','RAPIDAPI')),
 INDEX idx_trip_item_day(day_id,sort_order)
);
