(function () {
    'use strict';

    angular.module('businessServices')
            .controller('BusinessServicesEditController', ['$scope', '$location', '$window', '$log', '$filter', '$routeParams', 'BusinessServices', function ($scope, $location, $window, $log, $filter, $routeParams, BusinessServices) {
                    $log.debug('BusinessServicesEditController initializing...');
                    $log.debug('params:' + $routeParams.id);
                    $scope.bsToEdit = BusinessServices.get({id:$routeParams.id});
                    $log.debug("bsToEdit: " + $scope.bsToEdit.id + " " + $scope.bsToEdit.name);
                }]);
}());