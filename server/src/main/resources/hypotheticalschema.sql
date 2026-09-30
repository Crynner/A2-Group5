-- CREATE TABLE products (
--     id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
--     product_name TEXT NOT NULL,
--     size TEXT NOT NULL
-- );

-- CREATE TABLE allergens (
--     id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
--     allergen TEXT NOT NULL
-- );

-- CREATE TABLE products_allergens (
--     product_id INTEGER NOT NULL,
--     allergen_id INTEGER NOT NULL,
--     PRIMARY KEY (product_id, allergen_id),
--     FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
--     FOREIGN KEY (allergen_id) REFERENCES allergens(id) ON DELETE CASCADE
-- );

-- CREATE TABLE stores (
--     id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
--     store_name TEXT NOT NULL,
--     region TEXT,
--     store_address TEXT
-- );

-- CREATE TABLE products_stores (
--     product_id INTEGER NOT NULL,
--     store_id INTEGER NOT NULL,
--     PRIMARY KEY (product_id, store_id),
--     FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
--     FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
-- );