-- -- insert into products (product_id, name, brand, category, package_size, lactose_free, gluten_free, vegetarian, vegan) values
-- --   (1, 'Trim Milk', 'Anchor', 'Dairy', '2L', true, false, true, true),
-- --   (2, 'Original Mixed Grain Bread', 'Vogel''s', 'Bakery', '700g', false, true, true, true),
-- --   (3, 'Baked Beans', 'Wattie''s', 'Pantry', '420g', false, true, true, true);

-- -- insert into stores (store_id, store_name, city, region, address) values
-- --   (1, 'Woolworths Newmarket', 'Auckland', 'Auckland', 'Westfield Newmarket, 309 Broadway'),
-- --   (2, 'PAK''nSAVE Mt Albert', 'Auckland', 'Auckland', '1164 New North Road'),
-- --   (3, 'New World Victoria Park', 'Auckland', 'Auckland', '2 College Hill');


-- -- insert into store_prices (store_price_id, product_id, store_id, price) values
-- --   (1, 1, 1, 4.59),
-- --   (2, 1, 2, 4.39),
-- --   (3, 1, 3, 4.79),
-- --   (4, 2, 1, 6.49),
-- --   (5, 3, 2, 2.89);

-- -- =====================================================================
-- --  NZ Food Items Database -- seed data 
-- -- =====================================================================



-- INSERT INTO products (product_id, product_name, brand, category, package_size, lactose_free, gluten_free, vegetarian, vegan) VALUES
--     (1,  'Apple',             NULL,           'Produce',  '1kg bag',           TRUE,  TRUE,  TRUE,  TRUE),
--     (2,  'Broccoli',          NULL,           'Produce',  '500g head',         TRUE,  TRUE,  TRUE,  TRUE),
--     (3,  'Chicken Breast',    'Tegel',        'Meat',     '1kg',               TRUE,  TRUE,  FALSE, FALSE),
--     (4,  'Steak',             NULL,           'Meat',     '500g (rump)',       TRUE,  TRUE,  FALSE, FALSE),
--     (5,  'Trim Milk',         'Anchor',       'Dairy',    '2L',                FALSE, TRUE,  TRUE,  FALSE),
--     (6,  'Coke',              'Coca-Cola',    'Beverage', '1.5L',              TRUE,  TRUE,  TRUE,  TRUE),
--     (7,  'White Bread',       'Tip Top',      'Bakery',   '700g loaf',         TRUE,  FALSE, TRUE,  TRUE),
--     (8,  'Butter',            'Anchor',       'Dairy',    '500g block',        FALSE, TRUE,  TRUE,  FALSE),
--     (9,  'Pizza',             'Hell Frozen',  'Frozen',   '500g (pepperoni)',  FALSE, FALSE, FALSE, FALSE),
--     (10, 'Vegetarian Pizza',  'Hell Frozen',  'Frozen',   '500g',              FALSE, FALSE, TRUE,  FALSE);

-- INSERT INTO stores (store_id, store_name, region, address) VALUES
-- (1, 'Woolworths Newmarket',       'Auckland', 'Westfield Newmarket, 309 Broadway'),
-- (2, 'PAK''nSAVE Mt Albert',       'Auckland', '1164 New North Road'),
-- (3, 'New World Victoria Park',    'Auckland', '2 College Hill');

-- INSERT INTO store_prices (product_id, store_id, price) VALUES
--     (1, 1, 4.49), (1, 2, 3.99), (1, 3, 4.99),
--     (2, 1, 3.69), (2, 2, 3.29), (2, 3, 3.99),
--     (3, 1, 14.49), (3, 2, 12.99), (3, 3, 15.99),
--     (4, 1, 12.49), (4, 2, 10.99), (4, 3, 13.99),
--     (5, 1, 4.59), (5, 2, 4.39), (5, 3, 4.79),
--     (6, 1, 3.79), (6, 2, 3.29), (6, 3, 3.99),
--     (7, 1, 3.89), (7, 2, 3.49), (7, 3, 4.29),
--     (8, 1, 7.49), (8, 2, 6.49), (8, 3, 7.99),
--     (9, 1, 8.99), (9, 2, 6.99), (9, 3, 9.99),
--     (10, 1, 9.49), (10, 2, 7.49), (10, 3, 10.49);