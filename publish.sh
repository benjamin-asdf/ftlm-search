#!/bin/sh

rsync -avz run.sh  linode:/var/www/ftlm-search/
rsync -avz target linode:/var/www/ftlm-search/
rsync -avz release.jar linode:/var/www/ftlm-search/
rsync -avz prod-config.edn linode:/var/www/ftlm-search/config.edn
