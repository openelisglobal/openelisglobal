
#!/usr/bin/env python
# -*- coding: utf-8 -*-

def get_comma_split_names( name ):
    split_name_list = [name]

    if ',' in name:
        split_name_list = name.split(",")
    elif ';' in name:
        split_name_list = name.split(";")

    for i in range(0, len(split_name_list)):
        split_name_list[i] = split_name_list[i].strip()

    return split_name_list

def esc_name(name):
    if "'" in name:
        return name.replace("'", "''") 
    else:
        return name.strip()

old = []

old_file = open("input_files/currentDictNames.txt")
new_file = open("input_files/selectList.txt")
result = open("output_files/dictionaryResult.sql",'w')

for line in old_file:
    old.append(line.strip())
old_file.close()

for line in new_file:
    if len(line) > 1:
        values = get_comma_split_names(line)

        for value in values:
            if value.strip() not in old and value.strip() != 'n/a' and value.strip() != '':
                old.append(value.strip())
                result.write("INSERT INTO clinlims.dictionary ( id, is_active, dict_entry, lastupdated, dictionary_category_id, display_key ) \n\t")
                result.write("VALUES ( nextval( 'clinlims.dictionary_seq' ) , 'Y' , '" + esc_name(value) + "' , now(), ( select id from clinlims.dictionary_category where description = 'Kenya Lab'  ), 'dictionary.result."+ esc_name(value) + "' );\n")

result.close()

print "Done check dictionaryResult.sql for values"


