import sys
import csv
import json


def items_from_csv(path):
    with open(path, 'r') as f:
        reader = csv.DictReader(f, fieldnames=["vrn", "vehicle_category", "model", "date_of_retrofit"])
        for row in reader:
            yield row


if __name__ == "__main__":
    output_json_filename = "testdata.json"
    params = sys.argv
    if len(params) != 2:
        raise Exception("You have to provide csv file path! F.e. python csv_to_json.py myfile.csv")
    csv_path = params[1]
    items = items_from_csv(csv_path)
    items_list = list(items)
    print(json.dumps(items_list, sort_keys=True, indent=4))
    with open(output_json_filename, "w") as fout:
        json.dump(items_list, fout, sort_keys=True, indent=4)
