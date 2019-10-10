FROM nginx

COPY nginx.default /etc/nginx/sites-available/default
COPY winslow-ui-ng/. /var/www/html

