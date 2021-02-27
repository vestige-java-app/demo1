FROM gaellalire/vestige:10.0.2-alpine-openjdk8
RUN export VESTIGE_INSTALL_URL=https://gaellalire.fr/vestige/repository/demo1/demo1-2.0.0.xml \
 && export VESTIGE_STOP_AFTER_START=true \
 && export VESTIGE_INSTALL_M2_REPO=/tmp/m2Repository \
 && export VESTIGE_INSTALL_LOCAL_NAME=demo1 \
 && /opt/vestige/vestige \
 && sed -i -e 's/name=.*/name=world/' -e 's/port=.*/port=7777/' /root/.config/vestige/app/demo1/conf.properties \
 && rm -rf /tmp/m2Repository
CMD /opt/vestige/vestige
