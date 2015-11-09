(function () {
    'use strict';

    var MODULE_NAME = 'businessServices';

    /**
     * Function used to append an extra transformer to the default $http transforms.
     */
    function appendTransform(defaultTransform, transform) {
        defaultTransform = angular.isArray(defaultTransform) ? defaultTransform : [defaultTransform];
        return defaultTransform.concat(transform);
    }

    var bsCrudApp = angular.module(MODULE_NAME, ['ngResource', 'ngRoute']);

    bsCrudApp.config(function ($routeProvider) {
        $routeProvider
                .when('/',
                        {
                            controller: 'BusinessServicesController',
                            templateUrl: 'admin/bsm/main.html'
                        })
                .when('/index.jsp',
                        {
                            controller: 'BusinessServicesController',
                            templateUrl: 'main.html'
                        })
                .otherwise({redirectTo: '/hallo'});
    });

    /**
     * BusinessService REST $resource
     */
    bsCrudApp.factory('BusinessServices', function ($resource, $log, $http) {
        return $resource('api/v2/business-services/:id', {},
                {
                    'query': {
                        method: 'GET',
                        isArray: true,
                        // Append a transformation that will unwrap the item array
                        transformResponse: appendTransform($http.defaults.transformResponse, function (data, headers, status) {
                            // Always return the data as an array
                            return angular.isArray(data['business-service']) ? data['business-service'] : [data['business-service']];
                        })
                    }
                }
        );
    });

    /**
     * BusinessServices controller
     */
    bsCrudApp.controller('BusinessServicesController', ['$scope', '$location', '$window', '$log', '$filter', 'BusinessServices', function ($scope, $location, $window, $log, $filter, BusinessServices) {
            $log.debug('BusinessServicesController initializing...');

            // Fetch all of the items
            var listBS = function () {
                $log.debug("listBS called");
                BusinessServices.query(
                        {
                            limit: 0,
                            orderBy: 'name',
                            order: 'asc'
                        },
                        function (value, headers) {
                            $scope.items = value;
                        },
                        function (response) {
                            switch (response.status) {
                                case 404:
                                    // If we didn't find any elements, then clear the list
                                    $scope.items = [];
                                    break;
                                case 401:
                                case 403:
                                    // Handle session timeout by reloading page completely
                                    $window.location.href = $location.absUrl();
                                    break;
                            }
                        }
                );
            };
            listBS();

            $scope.bsCreate = function () {
                $log.debug("new bsCreate");
                $scope.newBS = new BusinessServices({name: $scope.name}).$save()
                        .then(function (newBS) {
                            $log.debug("then-OK: " + newBS);
                            listBS();
                        })
                        .catch(function (req) {
                            $log.debug("save new failed: " + req);
                        });
            };
            
            $scope.bsDelete = function (id) {
                $log.debug("bsDelete");
                $log.debug("bsDelete id: " + id);
                new BusinessServices().$delete({id:id})
                        .then(function (id) {
                            $log.debug("deleted " + id);
                            listBS();
                        })
                        .catch(function (req) {
                            $log.debug("delete failed for " + id);
                        });
            };
        }])
            .run(['$rootScope', '$log', function ($rootScope, $log) {
                    $log.debug('Finished initializing ' + MODULE_NAME);
                }]);

    angular.element(document).ready(function () {
        console.log('Bootstrapping ' + MODULE_NAME);
        angular.bootstrap(document, [MODULE_NAME]);
    });
}());
