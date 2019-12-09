FROM golang:1.12.0 AS builder
MAINTAINER barrtailor
# Create our application direcory
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY ./ .
RUN make build-go-arm
# While we're here in amd64, download the qemu-arm-static binary for the arm image in the next build step
RUN curl -L https://github.com/balena-io/qemu/releases/download/v3.0.0%2Bresin/qemu-3.0.0+resin-arm.tar.gz | tar zxvf - -C . && mv qemu-3.0.0+resin-arm/qemu-arm-static .

FROM arm32v7/node:12.13.1-alpine
COPY qemu-arm-static /usr/bin

# Expose Configuration Volume
VOLUME /mbs/config

# Copy and install dependencies
COPY package.json /usr/src/app/
COPY *.yml /mbs/config/
RUN npm install --production

# Copy everything else
COPY . /usr/src/app

# Set config directory
ENV CONFIG_DIR=/mbs/config

# Expose the web service port
EXPOSE 8080

# Run the service
CMD [ "npm", "start" ]

