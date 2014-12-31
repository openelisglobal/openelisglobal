#!/usr/bin/env python
# -*- coding: utf-8 -*-

organization = []
region = []
region_dict = {}
used = ['']
county_list = []
county_district_file = open('input_files/county_district.txt','r')
region_file = open('input_files/region.txt', 'r')

for line in county_district_file:
    if len(line) > 1:
        organization.append(line.strip(" "))
county_district_file.close()

for line in region_file:
    if len(line) > 1:
        region.append(line.strip(" "))
region_file.close()

for row in range(0, len(region)):
	#create dictionary for the county/region name and number
	county_list = region[row].split(",")
	region_dict[county_list[0].strip()] = county_list[1].strip()

def get_org_id(county):
    county_name = county.strip()
    if len(county_name) > 3:
	return region_dict[county_name]
    return 'NULL'

def get_org_type_id(org):
    for row in range(0, len(region)):
        region_name = region[row]
	region_field = region_name.split(",")
	if len(org) > 3 and org.strip() not in used and region_field[0].strip() == org.strip():
	   used.append(org.strip())
	   return '8'
    return '7'

def esc_name(name):
    if "'" in name:
        return name.replace("'", "''")
    else:
        return name.strip()

sql_insert = "INSERT INTO clinlims.organization( id, name, org_id, lastupdated, is_active) \n\t VALUES ("
count = 10
county_district_results = open("output_files/county_district.sql", 'w')
for row in range(0, len(organization)):
        county_district_name = organization[row]
	org_field = county_district_name.split(",")
        if org_field not in used and 'n/a' not in org_field:
            used.append(org_field)
            county_district_results.write(sql_insert)
            county_district_results.write(org_field[0] + ", '" + esc_name(org_field[1]) + "', " + get_org_id(org_field[2]) +", now(), 'Y');\n\t")
	    county_district_results.write("INSERT INTO clinlims.organization_organization_type( org_id, org_type_id) \n\t\t VALUES ( "+ org_field[0] +",'"+ get_org_type_id(org_field[1]) +"');\n")

print "Done Look for the results in county_district.sql"
