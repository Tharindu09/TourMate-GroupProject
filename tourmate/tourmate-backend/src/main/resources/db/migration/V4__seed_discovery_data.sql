-- Illustrative seed data. Prices are examples, not live quotes. Ratings remain zero until reviews exist.
INSERT INTO destinations (id,name,city,country,description,cover_image_url,latitude,longitude) VALUES
(1,'Ella','Ella','Sri Lanka','Slow mornings, tea-covered hills and unforgettable railway views. Discover the greener side of Sri Lanka.',NULL,6.8667,81.0466),
(2,'Kandy','Kandy','Sri Lanka','Lakeside walks, sacred temples and the cultural heart of the island.',NULL,7.2906,80.6337),
(3,'Galle','Galle','Sri Lanka','Wander the old fort, find a little cafe and watch the Indian Ocean roll in.',NULL,6.0328,80.2170),
(4,'Sigiriya','Sigiriya','Sri Lanka','Ancient rock fortresses, forest trails and wide-open landscapes.',NULL,7.9570,80.7603),
(5,'Nuwara Eliya','Nuwara Eliya','Sri Lanka','Cool mountain air, tea estates and gardens in the high country.',NULL,6.9497,80.7891),
(6,'Mirissa','Mirissa','Sri Lanka','Golden beaches, ocean breezes and sunsets worth slowing down for.',NULL,5.9483,80.4716),
(7,'Colombo','Colombo','Sri Lanka','Markets, local food and a lively oceanfront city.',NULL,6.9271,79.8612);
INSERT INTO places (destination_id,name,type,description,address,latitude,longitude,price_level,estimated_cost) VALUES
(1,'Nine Arch Bridge','ATTRACTION','A graceful stone railway bridge surrounded by lush hill country. Stay clear of the tracks and follow local access advice.','Demodara, Ella',6.8768,81.0608,0,0),
(1,'Little Adam’s Peak','ACTIVITY','A short hill-country walk with sweeping views. Allow time for breaks and changing weather.','Passara Road, Ella',6.8654,81.0630,0,0),
(1,'Ravana Falls','ATTRACTION','A waterfall stop along the road south of Ella. View from designated safe areas.','Wellawaya Road, Ella',6.8407,81.0545,0,0),
(1,'Hill Country Guesthouse (demo)','HOTEL','Illustrative accommodation listing for testing the booking-free discovery experience.','Ella town',6.8710,81.0470,2,12000),
(1,'Tea Garden Kitchen (demo)','RESTAURANT','Illustrative local-food listing with vegetarian options.','Ella town',6.8730,81.0490,1,2500),
(2,'Kandy Lake','ATTRACTION','A peaceful walk beside the lake in the centre of Kandy.','Kandy city centre',7.2900,80.6413,0,0),
(3,'Galle Fort','ATTRACTION','Explore lanes, historic walls and coastal views on foot.','Galle Fort',6.0269,80.2170,0,0),
(4,'Sigiriya Rock Fortress','ATTRACTION','An ancient fortress rising above the central plains. Check official ticket prices before visiting.','Sigiriya',7.9570,80.7603,3,11000),
(5,'Victoria Park','ATTRACTION','Garden paths and a relaxed afternoon in the cool highlands.','Nuwara Eliya',6.9684,80.7676,1,1000),
(6,'Mirissa Beach','ATTRACTION','A crescent of sand for a slow afternoon by the sea.','Mirissa',5.9430,80.4590,0,0);
INSERT INTO attraction_detail(place_id,suggested_duration_minutes,difficulty) VALUES (1,90,'EASY'),(3,45,'EASY'),(6,60,'EASY'),(7,120,'EASY'),(8,180,'MODERATE');
INSERT INTO hotel_detail(place_id,star_rating,check_in_time,check_out_time) VALUES(4,3,'14:00','11:00');
INSERT INTO restaurant_detail(place_id,cuisine,vegetarian_options) VALUES(5,'Sri Lankan',TRUE);
