FROM owasp/modsecurity-crs:nginx-alpine

USER root

# Cài libmagic
RUN apk add --no-cache file

USER nginx

# Copy nginx template
COPY default.conf /etc/nginx/templates/conf.d/default.conf.template

# SSL
RUN mkdir -p /etc/nginx/ssl
COPY ssl/cert.pem /etc/nginx/ssl/cert.pem
COPY ssl/key.pem /etc/nginx/ssl/key.pem

EXPOSE 80 443
