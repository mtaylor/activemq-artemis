/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * @module ARTEMIS
 */
var ARTEMIS = (function(ARTEMIS) {

    ARTEMIS.SessionsController = function ($scope, workspace, ARTEMISService, jolokia, localStorage) {

        var artemisJmxDomain = localStorage['artemisJmxDomain'] || "org.apache.activemq.artemis";

        $scope.workspace = workspace;
        $scope.sessions = [];
        $scope.totalServerItems = 0;
        $scope.pagingOptions = {
            pageSizes: [50, 100, 200],
            pageSize: 100,
            currentPage: 1
        };
        $scope.connectionFilter = {
            name: '',
            filter: '',
            sortColumn: '',
            sortOrder: ''
        };
        $scope.connectionFilterOptions = [
            { id: "noConsumer", name: "No Consumer" }
        ];
        $scope.connectionFilter;
        $scope.sortOptions = {
            fields: ["name"],
            directions: ["asc"]
        };
        var refreshed = false;
        var attributes = [
            {
                field: 'sessionID',
                displayName: 'ID',
                width: '*'
            },
            {
                field: 'connectionID',
                displayName: 'Connection ID',
                width: '*'
            },
            {
                field: 'consumerCount',
                displayName: 'Consumer Count',
                width: '*'
            },
            {
                field: 'creationTime',
                displayName: 'Creation Time',
                width: '*'
            }
        ];

        $scope.gridOptions = {
            selectedItems: [],
            data: 'sessions',
            showFooter: true,
            showFilter: true,
            showColumnMenu: true,
            enableCellSelection: false,
            enableHighlighting: true,
            enableColumnResize: true,
            enableColumnReordering: true,
            selectWithCheckboxOnly: false,
            showSelectionCheckbox: false,
            multiSelect: false,
            displaySelectionCheckbox: false,
            pagingOptions: $scope.pagingOptions,
            filterOptions: {
                filterText: '',
                useExternalFilter: true
            },
            enablePaging: true,
            totalServerItems: 'totalServerItems',
            maintainColumnRatios: false,
            columnDefs: attributes,
            enableFiltering: true,
            useExternalFiltering: true,
            sortInfo: $scope.sortOptions,
            useExternalSorting: true
        };
        $scope.refresh = function () {
            refreshed = true;
            $scope.loadTable();
        };
        $scope.loadTable = function () {
            $scope.connectionFilter.name = $scope.gridOptions.filterOptions.filterText;
            $scope.connectionFilter.sortColumn = $scope.sortOptions.fields[0];
            $scope.connectionFilter.sortOrder = $scope.sortOptions.directions[0];
            var mbean = getBrokerMBean(jolokia);
            if (mbean) {
                var method = 'listSessions(java.lang.String, int, int)';
                jolokia.request({ type: 'exec', mbean: mbean, operation: method, arguments: [JSON.stringify($scope.connectionFilter), $scope.pagingOptions.currentPage, $scope.pagingOptions.pageSize] }, onSuccess(populateTable, { error: onError }));
            }
        };
        function onError() {
            Core.notification("error", "Could not retrieve session list from broker.");
            $scope.workspace.selectParentNode();
        }
        function populateTable(response) {
            var data = JSON.parse(response.value);
            $scope.sessions = [];
            angular.forEach(data, function (value, idx) {
                $scope.sessions.push(value);
            });
            $scope.totalServerItems = data["count"];
            if (refreshed == true) {
                $scope.gridOptions.pagingOptions.currentPage = 1;
                refreshed = false;
            }
            Core.$apply($scope);
        }
        $scope.$watch('sortOptions', function (newVal, oldVal) {
            if (newVal !== oldVal) {
                $scope.loadTable();
            }
        }, true);
        $scope.$watch('pagingOptions', function (newVal, oldVal) {
            if (parseInt(newVal.currentPage) && newVal !== oldVal && newVal.currentPage !== oldVal.currentPage) {
                $scope.loadTable();
            }
        }, true);

        function getBrokerMBean(jolokia) {
            var mbean = null;
            var selection = workspace.selection;
            var folderNames = selection.folderNames;
            mbean = "" + folderNames[0] + ":broker=" + folderNames[1];
            ARTEMIS.log.info("broker=" + mbean);
            return mbean;
        }
    };
    return ARTEMIS;
} (ARTEMIS || {}));
