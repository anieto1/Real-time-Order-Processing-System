
CREATE DATABASE orderdb;
CREATE DATABASE inventorydb;
CREATE DATABASE paymentdb;
CREATE DATABASE notificationdb;


GRANT ALL PRIVILEGES ON DATABASE orderdb TO admin;
GRANT ALL PRIVILEGES ON DATABASE inventorydb TO admin;
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO admin;
GRANT ALL PRIVILEGES ON DATABASE notificationdb TO admin;

\echo '================================'
\echo 'All databases created successfully!'
