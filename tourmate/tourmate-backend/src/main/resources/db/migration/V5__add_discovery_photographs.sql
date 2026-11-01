-- Local photos are served by the frontend. Keep existing custom cover images.
UPDATE destinations SET cover_image_url = '/images/ella.jpg' WHERE name = 'Ella' AND cover_image_url IS NULL;
UPDATE destinations SET cover_image_url = '/images/kandy.jpg' WHERE name = 'Kandy' AND cover_image_url IS NULL;
UPDATE destinations SET cover_image_url = '/images/galle-fort.jpg' WHERE name = 'Galle' AND cover_image_url IS NULL;
UPDATE destinations SET cover_image_url = '/images/sigiriya.jpg' WHERE name = 'Sigiriya' AND cover_image_url IS NULL;
UPDATE destinations SET cover_image_url = '/images/nuwara-eliya.jpg' WHERE name = 'Nuwara Eliya' AND cover_image_url IS NULL;
UPDATE destinations SET cover_image_url = '/images/mirissa.jpg' WHERE name = 'Mirissa' AND cover_image_url IS NULL;
UPDATE destinations SET cover_image_url = '/images/colombo.png' WHERE name = 'Colombo' AND cover_image_url IS NULL;
UPDATE places SET cover_image_url = '/images/nine-arch-bridge.jpg' WHERE name = 'Nine Arch Bridge' AND cover_image_url IS NULL;
UPDATE places SET cover_image_url = '/images/little-adams-peak.jpg' WHERE name = 'Little Adam’s Peak' AND cover_image_url IS NULL;
UPDATE places SET cover_image_url = '/images/ravana-falls.jpg' WHERE name = 'Ravana Falls' AND cover_image_url IS NULL;
UPDATE places SET cover_image_url = '/images/ella.jpg' WHERE name = 'Hill Country Guesthouse (demo)' AND cover_image_url IS NULL;
UPDATE places SET cover_image_url = '/images/ella.jpg' WHERE name = 'Tea Garden Kitchen (demo)' AND cover_image_url IS NULL;
UPDATE places SET cover_image_url = '/images/kandy-lake.jpg' WHERE name = 'Kandy Lake' AND cover_image_url IS NULL;
UPDATE places SET cover_image_url = '/images/galle-fort.jpg' WHERE name = 'Galle Fort' AND cover_image_url IS NULL;
UPDATE places SET cover_image_url = '/images/sigiriya.jpg' WHERE name = 'Sigiriya Rock Fortress' AND cover_image_url IS NULL;
UPDATE places SET cover_image_url = '/images/victoria-park.jpg' WHERE name = 'Victoria Park' AND cover_image_url IS NULL;
UPDATE places SET cover_image_url = '/images/mirissa.jpg' WHERE name = 'Mirissa Beach' AND cover_image_url IS NULL;
